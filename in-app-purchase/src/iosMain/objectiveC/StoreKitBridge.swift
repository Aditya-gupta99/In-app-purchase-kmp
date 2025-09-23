// File: shared/src/iosMain/objectiveC/StoreKitBridge.swift
import StoreKit
import Foundation

@objc
public class StoreKitBridge: NSObject {

    @objc public static let shared = StoreKitBridge()
    @objc public weak var delegate: StoreKitBridgeDelegate?

    private var paymentQueue: SKPaymentQueue?
    private var productCache: [String: SKProduct] = [:]
    private var isInitialized = false
    private var currentProductsRequest: SKProductsRequest?
    private var currentProductsDelegate: ProductsRequestDelegate?

    // Store completion handlers for async operations
    private var initializeCompletion: ((Bool, String?) -> Void)?
    private var productsCompletion: ((NSDictionary, NSArray, String?) -> Void)?
    private var purchaseCompletion: ((Bool, String?) -> Void)?
    private var restoreCompletion: ((NSDictionary, String?) -> Void)?

    private override init() {
        super.init()
    }

    // MARK: - Initialization
    @objc public func initialize(completion: @escaping (Bool, String?) -> Void) {
        initializeCompletion = completion

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            guard SKPaymentQueue.canMakePayments() else {
                self.initializeCompletion?(false, "In-app purchases are disabled")
                self.initializeCompletion = nil
                return
            }

            self.paymentQueue = SKPaymentQueue.default()
            self.paymentQueue?.add(self)
            self.isInitialized = true

            print("✅ StoreKit initialized successfully")
            self.initializeCompletion?(true, nil)
            self.initializeCompletion = nil
        }
    }

    // MARK: - Product Queries
    @objc public func queryProducts(productIds: [String], completion: @escaping (NSDictionary, NSArray, String?) -> Void) {
        guard isInitialized else {
            completion([:], [], "StoreKit not initialized")
            return
        }

        print("🛒 Querying products: \(productIds)")

        let productIdentifiers = Set(productIds)
        let request = SKProductsRequest(productIdentifiers: productIdentifiers)

        let delegate = ProductsRequestDelegate()
        self.currentProductsDelegate = delegate // Keep strong reference

        delegate.completion = { [weak self] validProducts, invalidIds, error in
            if let error = error {
                completion([:], [], error)
                return
            }

            // Cache products and convert to dictionary format
            let productsDict = NSMutableDictionary()

            for product in validProducts {
                self?.productCache[product.productIdentifier] = product

                let formatter = NumberFormatter()
                formatter.numberStyle = .currency
                formatter.locale = product.priceLocale

                let productData: [String: Any] = [
                    "id": product.productIdentifier,
                    "title": product.localizedTitle,
                    "description": product.localizedDescription,
                    "price": formatter.string(from: product.price) ?? product.price.stringValue,
                    "priceCurrencyCode": product.priceLocale.currencyCode ?? "",
                    "priceAmountMicros": Int64(product.price.doubleValue * 1_000_000),
                    "type": "inapp"
                ]
                productsDict[product.productIdentifier] = productData
            }

            print("✅ Found \(validProducts.count) valid products")
            if !invalidIds.isEmpty {
                print("❌ Invalid product IDs: \(invalidIds)")
            }

            completion(productsDict, NSArray(array: invalidIds), nil)
        }

        request.delegate = delegate
        self.currentProductsRequest = request // Keep strong reference
        request.start()
    }

    // MARK: - Purchase Flow
    @objc public func launchPurchase(productId: String, completion: @escaping (Bool, String?) -> Void) {
        guard isInitialized else {
            completion(false, "StoreKit not initialized")
            return
        }

        guard let product = productCache[productId] else {
            completion(false, "Product not found: \(productId). Call queryProducts first.")
            return
        }

        print("🛒 Launching purchase for: \(productId)")

        purchaseCompletion = completion
        let payment = SKPayment(product: product)
        paymentQueue?.add(payment)
    }

    // MARK: - Purchase Management
    @objc public func restorePurchases(completion: @escaping (NSDictionary, String?) -> Void) {
        guard isInitialized else {
            completion([:], "StoreKit not initialized")
            return
        }

        print("🔄 Restoring purchases...")

        let restoredPurchases = NSMutableDictionary()
        var restoreCompleted = false

        // Create a temporary observer for restore
        let restoreObserver = RestoreObserver { [weak self] transactions, error in
            guard !restoreCompleted else { return }
            restoreCompleted = true

            if let error = error {
                completion([:], error)
                return
            }

            for (index, transaction) in transactions.enumerated() {
                let purchaseData: [String: Any] = [
                    "productId": transaction.payment.productIdentifier,
                    "purchaseToken": transaction.transactionIdentifier ?? "",
                    "orderId": transaction.transactionIdentifier ?? "",
                    "purchaseTime": Int64((transaction.transactionDate?.timeIntervalSince1970 ?? 0) * 1000),
                    "isAcknowledged": true
                ]
                restoredPurchases["purchase_\(index)"] = purchaseData

                // Finish the transaction
                self?.paymentQueue?.finishTransaction(transaction)
            }

            completion(restoredPurchases, nil)
        }

        paymentQueue?.add(restoreObserver)
        paymentQueue?.restoreCompletedTransactions()
    }

    @objc public func disconnect() {
        paymentQueue?.remove(self)
        paymentQueue = nil
        productCache.removeAll()
        isInitialized = false
        currentProductsRequest = nil
        currentProductsDelegate = nil

        // Clear completion handlers
        initializeCompletion = nil
        purchaseCompletion = nil

        print("🔌 StoreKit disconnected")
    }
}

// MARK: - SKPaymentTransactionObserver
extension StoreKitBridge: SKPaymentTransactionObserver {
    public func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        for transaction in transactions {
            switch transaction.transactionState {
            case .purchased:
                handlePurchaseSuccess(transaction)
            case .restored:
                handleRestoreSuccess(transaction)
            case .failed:
                handlePurchaseFailed(transaction)
            case .purchasing:
                print("🔄 Purchasing: \(transaction.payment.productIdentifier)")
            case .deferred:
                print("⏳ Purchase deferred: \(transaction.payment.productIdentifier)")
            @unknown default:
                print("❓ Unknown transaction state: \(transaction.payment.productIdentifier)")
            }
        }
    }

    private func handlePurchaseSuccess(_ transaction: SKPaymentTransaction) {
        let productId = transaction.payment.productIdentifier
        let purchaseToken = transaction.transactionIdentifier ?? ""
        let orderId = transaction.transactionIdentifier ?? ""
        let purchaseTime = Int64((transaction.transactionDate?.timeIntervalSince1970 ?? 0) * 1000)

        print("✅ Purchase successful: \(productId)")

        // Notify delegate
        delegate?.onPurchaseUpdate(
            productId: productId,
            purchaseToken: purchaseToken,
            orderId: orderId,
            purchaseTime: purchaseTime
        )

        // Complete purchase flow
        purchaseCompletion?(true, nil)
        purchaseCompletion = nil

        // Finish transaction
        paymentQueue?.finishTransaction(transaction)
    }

    private func handleRestoreSuccess(_ transaction: SKPaymentTransaction) {
        let productId = transaction.payment.productIdentifier
        let purchaseToken = transaction.transactionIdentifier ?? ""
        let orderId = transaction.transactionIdentifier ?? ""
        let purchaseTime = Int64((transaction.transactionDate?.timeIntervalSince1970 ?? 0) * 1000)

        print("✅ Restore successful: \(productId)")

        // Notify delegate
        delegate?.onPurchaseUpdate(
            productId: productId,
            purchaseToken: purchaseToken,
            orderId: orderId,
            purchaseTime: purchaseTime
        )
    }

    private func handlePurchaseFailed(_ transaction: SKPaymentTransaction) {
        let productId = transaction.payment.productIdentifier
        let error = transaction.error?.localizedDescription ?? "Purchase failed"

        print("❌ Purchase failed: \(productId) - \(error)")

        // Notify delegate
        delegate?.onPurchaseError(productId: productId, error: error)

        // Complete purchase flow
        purchaseCompletion?(false, error)
        purchaseCompletion = nil

        // Finish transaction
        paymentQueue?.finishTransaction(transaction)
    }
}

// MARK: - Helper Classes
private class ProductsRequestDelegate: NSObject, SKProductsRequestDelegate {
    var completion: (([SKProduct], [String], String?) -> Void)?

    func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        let validProducts = Array(response.products)
        let invalidIds = Array(response.invalidProductIdentifiers)
        completion?(validProducts, invalidIds, nil)
    }

    func request(_ request: SKRequest, didFailWithError error: Error) {
        completion?([], [], error.localizedDescription)
    }
}

private class RestoreObserver: NSObject, SKPaymentTransactionObserver {
    private let completion: ([SKPaymentTransaction], String?) -> Void
    private var restoredTransactions: [SKPaymentTransaction] = []

    init(completion: @escaping ([SKPaymentTransaction], String?) -> Void) {
        self.completion = completion
        super.init()
    }

    func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        for transaction in transactions {
            if transaction.transactionState == .restored {
                restoredTransactions.append(transaction)
            }
        }
    }

    func paymentQueueRestoreCompletedTransactionsFinished(_ queue: SKPaymentQueue) {
        queue.remove(self)
        completion(restoredTransactions, nil)
    }

    func paymentQueue(_ queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError error: Error) {
        queue.remove(self)
        completion([], error.localizedDescription)
    }
}