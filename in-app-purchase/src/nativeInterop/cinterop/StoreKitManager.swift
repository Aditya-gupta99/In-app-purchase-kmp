import StoreKit
import Foundation

// MARK: - C-compatible structures
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
    @objc public let data: NSObject?

    public init(success: Bool, errorMessage: String? = nil, errorCode: Int32 = 0, data: NSObject? = nil) {
        self.success = success
        self.errorMessage = errorMessage
        self.errorCode = errorCode
        self.data = data
        super.init()
    }
}

// MARK: - Callback typedefs for C interop
public typealias StoreKitInitCallback = @convention(c) (StoreKitResult) -> Void
public typealias StoreKitProductsCallback = @convention(c) (StoreKitResult) -> Void
public typealias StoreKitPurchaseCallback = @convention(c) (StoreKitResult) -> Void
public typealias StoreKitPurchaseUpdateCallback = @convention(c) (StoreKitPurchase) -> Void

// MARK: - StoreKit 2 Manager
@available(iOS 15.0, *)
@objc public class StoreKitManager: NSObject {

    private var products: [Product] = []
    private var purchaseUpdateCallback: StoreKitPurchaseUpdateCallback?
    private var transactionListener: Task<Void, Never>?

    @objc public static let shared = StoreKitManager()

    private override init() {
        super.init()
        startTransactionListener()
    }

    deinit {
        transactionListener?.cancel()
    }

    // MARK: - iOS Version Check
    @available(iOS 15.0, *)
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
    @available(iOS 15.0, *)
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

    @available(iOS 15.0, *)
    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified:
            throw StoreError.failedVerification
        case .verified(let safe):
            return safe
        }
    }

    @available(iOS 15.0, *)
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

    // MARK: - Public C-compatible methods

    @objc public func initialize(callback: @escaping StoreKitInitCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        let result = StoreKitResult(success: true)
        callback(result)
    }

    @objc public func getProducts(productIds: [String], callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        if #available(iOS 15.0, *) {
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
    }

    @objc public func launchPurchaseFlow(productId: String, callback: @escaping StoreKitPurchaseCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        if #available(iOS 15.0, *) {
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
    }

    @objc public func consumePurchase(purchaseToken: String, callback: @escaping StoreKitPurchaseCallback) {
        // In StoreKit 2, consumables are automatically consumed when the transaction is finished
        let result = StoreKitResult(success: true)
        callback(result)
    }

    @objc public func acknowledgePurchase(purchaseToken: String, callback: @escaping StoreKitPurchaseCallback) {
        // In StoreKit 2, purchases are automatically acknowledged when the transaction is finished
        let result = StoreKitResult(success: true)
        callback(result)
    }

    @objc public func getPurchases(callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        if #available(iOS 15.0, *) {
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
    }

    @objc public func restorePurchases(callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            let result = StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1)
            callback(result)
            return
        }

        if #available(iOS 15.0, *) {
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
    }

    @objc public func setPurchaseUpdateCallback(callback: @escaping StoreKitPurchaseUpdateCallback) {
        self.purchaseUpdateCallback = callback
    }

    @objc public func disconnect() {
        transactionListener?.cancel()
        transactionListener = nil
        purchaseUpdateCallback = nil
        products.removeAll()
    }
}

// MARK: - C-compatible functions
@available(iOS 15.0, *)
@_cdecl("storekit_initialize")
public func storekit_initialize(callback: @escaping StoreKitInitCallback) {
    StoreKitManager.shared.initialize(callback: callback)
}

@_cdecl("storekit_get_products")
@available(iOS 15.0, *)
public func storekit_get_products(productIds: UnsafePointer<UnsafePointer<CChar>?>, count: Int32, callback: @escaping StoreKitProductsCallback) {
    var productIdStrings: [String] = []
    for i in 0..<Int(count) {
        if let cString = productIds[i] {
            let string = String(cString: cString)
            productIdStrings.append(string)
        }
    }
    StoreKitManager.shared.getProducts(productIds: productIdStrings, callback: callback)
}

@_cdecl("storekit_launch_purchase_flow")
@available(iOS 15.0, *)
public func storekit_launch_purchase_flow(productId: UnsafePointer<CChar>, callback: @escaping StoreKitPurchaseCallback) {
    let productIdString = String(cString: productId)
    StoreKitManager.shared.launchPurchaseFlow(productId: productIdString, callback: callback)
}

@_cdecl("storekit_consume_purchase")
@available(iOS 15.0, *)
public func storekit_consume_purchase(purchaseToken: UnsafePointer<CChar>, callback: @escaping StoreKitPurchaseCallback) {
    let purchaseTokenString = String(cString: purchaseToken)
    StoreKitManager.shared.consumePurchase(purchaseToken: purchaseTokenString, callback: callback)
}

@_cdecl("storekit_acknowledge_purchase")
@available(iOS 15.0, *)
public func storekit_acknowledge_purchase(purchaseToken: UnsafePointer<CChar>, callback: @escaping StoreKitPurchaseCallback) {
    let purchaseTokenString = String(cString: purchaseToken)
    StoreKitManager.shared.acknowledgePurchase(purchaseToken: purchaseTokenString, callback: callback)
}

@_cdecl("storekit_get_purchases")
@available(iOS 15.0, *)
public func storekit_get_purchases(callback: @escaping StoreKitProductsCallback) {
    StoreKitManager.shared.getPurchases(callback: callback)
}

@_cdecl("storekit_restore_purchases")
@available(iOS 15.0, *)
public func storekit_restore_purchases(callback: @escaping StoreKitProductsCallback) {
    StoreKitManager.shared.restorePurchases(callback: callback)
}

@_cdecl("storekit_set_purchase_update_callback")
@available(iOS 15.0, *)
public func storekit_set_purchase_update_callback(callback: @escaping StoreKitPurchaseUpdateCallback) {
    StoreKitManager.shared.setPurchaseUpdateCallback(callback: callback)
}

@_cdecl("storekit_disconnect")
@available(iOS 15.0, *)
public func storekit_disconnect() {
    StoreKitManager.shared.disconnect()
}

enum StoreError: Error {
    case failedVerification
}