import StoreKit
import Foundation

// MARK: - @objc compatible model classes
@objc public class StoreKitProduct: NSObject {
    @objc public let id: String
    @objc public let displayName: String
    @objc public let productDescription: String
    @objc public let price: String
    @objc public let currencyCode: String
    @objc public let priceAmountMicros: Int64
    @objc public let type: String

    public init(id: String, displayName: String, description: String, price: String, currencyCode: String, priceAmountMicros: Int64, type: String) {
        self.id = id
        self.displayName = displayName
        self.productDescription = description
        self.price = price
        self.currencyCode = currencyCode
        self.priceAmountMicros = priceAmountMicros
        self.type = type
        super.init()
    }
}

@objc public class StoreKitPurchase: NSObject {
    @objc public let productId: String
    @objc public let purchaseToken: String
    @objc public let orderId: String
    @objc public let purchaseTime: Int64
    @objc public let isAcknowledged: Bool
    @objc public let originalJson: String
    @objc public let signature: String

    public init(productId: String, purchaseToken: String, orderId: String, purchaseTime: Int64, isAcknowledged: Bool, originalJson: String, signature: String) {
        self.productId = productId
        self.purchaseToken = purchaseToken
        self.orderId = orderId
        self.purchaseTime = purchaseTime
        self.isAcknowledged = isAcknowledged
        self.originalJson = originalJson
        self.signature = signature
        super.init()
    }
}

@objc public class StoreKitResult: NSObject {
    @objc public let success: Bool
    @objc public let errorMessage: String?
    @objc public let errorCode: Int32
    @objc public let data: NSArray?

    public init(success: Bool, errorMessage: String? = nil, errorCode: Int32 = 0, data: NSArray? = nil) {
        self.success = success
        self.errorMessage = errorMessage
        self.errorCode = errorCode
        self.data = data
        super.init()
    }
}

// MARK: - @objc callback type aliases
public typealias StoreKitInitCallback = (StoreKitResult) -> Void
public typealias StoreKitProductsCallback = (StoreKitResult) -> Void
public typealias StoreKitPurchaseCallback = (StoreKitResult) -> Void
public typealias StoreKitPurchaseUpdateCallback = (StoreKitPurchase) -> Void

// MARK: - Internal StoreKit 2 Manager
@available(iOS 15.0, *)
internal class StoreKitManager {

    private var products: [Product] = []
    private var purchaseUpdateCallback: StoreKitPurchaseUpdateCallback?
    private var transactionListener: Task<Void, Never>?

    static let shared = StoreKitManager()

    private init() {
        startTransactionListener()
    }

    deinit {
        transactionListener?.cancel()
    }

    // MARK: - iOS Version Check
    private func isStoreKit2Available() -> Bool {
        return true
    }

    private func checkiOSVersion() -> Bool {
        if #available(iOS 15.0, *) {
            return isStoreKit2Available()
        } else {
            return false
        }
    }

    // MARK: - Transaction Listener
    private func startTransactionListener() {
        transactionListener = Task {
            for await result in Transaction.updates {
                do {
                    let transaction = try checkVerified(result)
                    await handleTransaction(transaction)
                } catch {
                    print("Transaction verification failed: \(error)")
                }
            }
        }
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified:
            throw StoreError.failedVerification
        case .verified(let safe):
            return safe
        }
    }

    private func handleTransaction(_ transaction: Transaction) async {
        guard let callback = purchaseUpdateCallback else { return }

        let purchase = StoreKitPurchase(
            productId: transaction.productID,
            purchaseToken: String(transaction.id),
            orderId: String(transaction.id),
            purchaseTime: Int64(transaction.purchaseDate.timeIntervalSince1970 * 1000),
            isAcknowledged: true,
            originalJson: "",
            signature: ""
        )

        callback(purchase)

        // Finish the transaction
        await transaction.finish()
    }

    // MARK: - Internal methods with @objc callbacks

    func initialize(callback: @escaping StoreKitInitCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        let result = StoreKitResult(success: true)
        callback(result)
    }

    func getProducts(productIds: [String], callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        Task {
            do {
                let storeProducts = try await Product.products(for: productIds)
                self.products = storeProducts

                let productArray = NSMutableArray()
                for product in storeProducts {
                    let priceAmountMicros = Int64((product.price as NSDecimalNumber).doubleValue * 1_000_000)
                    let storeKitProduct = StoreKitProduct(
                        id: product.id,
                        displayName: product.displayName,
                        description: product.description,
                        price: product.displayPrice,
                        currencyCode: product.priceFormatStyle.currencyCode ?? "",
                        priceAmountMicros: priceAmountMicros,
                        type: product.type == .consumable || product.type == .nonConsumable ? "inapp" : "subs"
                    )
                    productArray.add(storeKitProduct)
                }

                let result = StoreKitResult(success: true, data: productArray)
                callback(result)
            } catch {
                let result = StoreKitResult(success: false, errorMessage: error.localizedDescription, errorCode: -1)
                callback(result)
            }
        }
    }

    func launchPurchaseFlow(productId: String, callback: @escaping StoreKitPurchaseCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        guard let product = products.first(where: { $0.id == productId }) else {
            let result = StoreKitResult(success: false, errorMessage: "Product not found", errorCode: -1)
            callback(result)
            return
        }

        Task {
            do {
                let result = try await product.purchase()

                switch result {
                case .success(let verification):
                    let transaction = try checkVerified(verification)
                    await transaction.finish()

                    let storeKitResult = StoreKitResult(success: true)
                    callback(storeKitResult)

                case .userCancelled:
                    let storeKitResult = StoreKitResult(success: false, errorMessage: "User cancelled", errorCode: 2)
                    callback(storeKitResult)

                case .pending:
                    let storeKitResult = StoreKitResult(success: false, errorMessage: "Purchase pending", errorCode: 3)
                    callback(storeKitResult)

                @unknown default:
                    let storeKitResult = StoreKitResult(success: false, errorMessage: "Unknown result", errorCode: -1)
                    callback(storeKitResult)
                }
            } catch {
                let storeKitResult = StoreKitResult(success: false, errorMessage: error.localizedDescription, errorCode: -1)
                callback(storeKitResult)
            }
        }
    }

    func consumePurchase(purchaseToken: String, callback: @escaping StoreKitPurchaseCallback) {
        // In StoreKit 2, consumables are automatically consumed when the transaction is finished
        let result = StoreKitResult(success: true)
        callback(result)
    }

    func acknowledgePurchase(purchaseToken: String, callback: @escaping StoreKitPurchaseCallback) {
        // In StoreKit 2, purchases are automatically acknowledged when the transaction is finished
        let result = StoreKitResult(success: true)
        callback(result)
    }

    func getPurchases(callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        Task {
            var purchases: [StoreKitPurchase] = []

            for await result in Transaction.currentEntitlements {
                do {
                    let transaction = try checkVerified(result)
                    let purchase = StoreKitPurchase(
                        productId: transaction.productID,
                        purchaseToken: String(transaction.id),
                        orderId: String(transaction.id),
                        purchaseTime: Int64(transaction.purchaseDate.timeIntervalSince1970 * 1000),
                        isAcknowledged: true,
                        originalJson: "",
                        signature: ""
                    )
                    purchases.append(purchase)
                } catch {
                    print("Transaction verification failed: \(error)")
                }
            }

            let purchaseArray = NSMutableArray()
            for purchase in purchases {
                purchaseArray.add(purchase)
            }

            let result = StoreKitResult(success: true, data: purchaseArray)
            callback(result)
        }
    }

    func restorePurchases(callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        Task {
            do {
                try await AppStore.sync()
                // After sync, get current purchases
                getPurchases(callback: callback)
            } catch {
                let result = StoreKitResult(success: false, errorMessage: error.localizedDescription, errorCode: -1)
                callback(result)
            }
        }
    }

    func setPurchaseUpdateCallback(callback: @escaping StoreKitPurchaseUpdateCallback) {
        self.purchaseUpdateCallback = callback
    }

    func disconnect() {
        transactionListener?.cancel()
        transactionListener = nil
        purchaseUpdateCallback = nil
        products.removeAll()
    }
}

// MARK: - Public @objc Wrapper for Kotlin
@available(iOS 15.0, *)
@objc public class StoreKitManagerWrapper: NSObject {

    private let manager = StoreKitManager.shared

    @objc public override init() {
        super.init()
    }

    @objc public func initialize(completion: @escaping (StoreKitResult) -> Void) {
        manager.initialize(callback: completion)
    }

    @objc public func getProducts(productIds: [String], completion: @escaping (StoreKitResult) -> Void) {
        manager.getProducts(productIds: productIds, callback: completion)
    }

    @objc public func launchPurchaseFlow(productId: String, completion: @escaping (StoreKitResult) -> Void) {
        manager.launchPurchaseFlow(productId: productId, callback: completion)
    }

    @objc public func consumePurchase(purchaseToken: String, completion: @escaping (StoreKitResult) -> Void) {
        manager.consumePurchase(purchaseToken: purchaseToken, callback: completion)
    }

    @objc public func acknowledgePurchase(purchaseToken: String, completion: @escaping (StoreKitResult) -> Void) {
        manager.acknowledgePurchase(purchaseToken: purchaseToken, callback: completion)
    }

    @objc public func getPurchases(completion: @escaping (StoreKitResult) -> Void) {
        manager.getPurchases(callback: completion)
    }

    @objc public func restorePurchases(completion: @escaping (StoreKitResult) -> Void) {
        manager.restorePurchases(callback: completion)
    }

    @objc public func setPurchaseUpdateCallback(callback: @escaping (StoreKitPurchase) -> Void) {
        manager.setPurchaseUpdateCallback(callback: callback)
    }

    @objc public func disconnect() {
        manager.disconnect()
    }
}

// MARK: - Error definitions
enum StoreError: Error {
    case failedVerification
}
