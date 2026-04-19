import Foundation
import StoreKit

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
    @objc public let transactionId: String
    @objc public let jwsRepresentation: String
    @objc public let originalTransactionId: String

    public init(productId: String, purchaseToken: String, orderId: String, purchaseTime: Int64, isAcknowledged: Bool, originalJson: String, signature: String, transactionId: String, jwsRepresentation: String, originalTransactionId: String) {
        self.productId = productId
        self.purchaseToken = purchaseToken
        self.orderId = orderId
        self.purchaseTime = purchaseTime
        self.isAcknowledged = isAcknowledged
        self.originalJson = originalJson
        self.signature = signature
        self.transactionId = transactionId
        self.jwsRepresentation = jwsRepresentation
        self.originalTransactionId = originalTransactionId
        super.init()
    }
}

@objc public class StoreKitResult: NSObject {
    @objc public let success: Bool
    @objc public let errorMessage: String?
    @objc public let errorCode: Int32
    @objc public let data: NSArray?
    @objc public let jwsRepresentation: String?

    public init(success: Bool, errorMessage: String? = nil, errorCode: Int32 = 0, data: NSArray? = nil, jwsRepresentation: String? = nil) {
        self.success = success
        self.errorMessage = errorMessage
        self.errorCode = errorCode
        self.data = data
        self.jwsRepresentation = jwsRepresentation
        super.init()
    }
}

// MARK: - Callback type aliases

public typealias StoreKitInitCallback           = (StoreKitResult) -> Void
public typealias StoreKitProductsCallback       = (StoreKitResult) -> Void
public typealias StoreKitPurchaseCallback       = (StoreKitResult) -> Void
public typealias StoreKitPurchaseUpdateCallback = (StoreKitPurchase) -> Void

// MARK: - StoreKit 2 Manager

@available(iOS 15.0, *)
internal class StoreKitManager {

    private var products: [Product] = []
    private var purchaseUpdateCallback: StoreKitPurchaseUpdateCallback?
    private var transactionListener: Task<Void, Never>?
    private var handledTransactionIds: Set<UInt64> = []
    private var currentUserId: UUID?

    static let shared = StoreKitManager()

    private init() {
        startTransactionListener()
    }

    deinit {
        transactionListener?.cancel()
    }

    // MARK: - Launch Drain

    private func drainStaleTransactionsAtLaunch() async {
        guard let currentUUID = currentUserId else {
            print("⏭️ [IAP] Launch drain: skipped — no currentUserId set")
            return
        }

        var count = 0
        for await result in Transaction.unfinished {
            do {
                let transaction = try checkVerified(result)

                if transaction.revocationDate != nil {
                    print("🚫 [IAP] Launch drain: finishing revoked \(transaction.id) (\(transaction.productID))")
                    await transaction.finish()
                    count += 1
                    continue
                }

                // Finish transactions that belong to a DIFFERENT user
                if let token = transaction.appAccountToken, token != currentUUID {
                    print("🧹 [IAP] Launch drain: finishing foreign \(transaction.id) (\(transaction.productID)) token=\(token) != current=\(currentUUID)")
                    await transaction.finish()
                    count += 1
                    continue
                }

                // Transactions with no token or matching token — leave them for recovery
                print("⏭️ [IAP] Launch drain: keeping transaction \(transaction.id) (\(transaction.productID))")

            } catch {
                if case .unverified(let t, _) = result {
                    print("⚠️ [IAP] Launch drain: finishing unverified transaction")
                    await t.finish()
                    count += 1
                }
            }
        }
        print("✅ [IAP] Launch drain complete — cleared \(count) stale transaction(s)")
    }

    // MARK: - Transaction Listener

    private func startTransactionListener() {
        transactionListener = Task {
            for await result in Transaction.updates {
                do {
                    let transaction = try checkVerified(result)

                    if handledTransactionIds.contains(transaction.id) {
                        print("⏭️ [IAP] Listener: skipping already-handled \(transaction.id)")
                        await transaction.finish()
                        continue
                    }

                    if transaction.revocationDate != nil {
                        print("🚫 [IAP] Listener: finishing revoked \(transaction.id)")
                        await transaction.finish()
                        continue
                    }

                    if let currentUUID = currentUserId,
                       let transactionToken = transaction.appAccountToken,
                       transactionToken != currentUUID {
                        print("🧹 [IAP] Listener: draining foreign \(transaction.id)")
                        await transaction.finish()
                        continue
                    }

                    if #available(iOS 17.0, *) {
                        if transaction.reason == .renewal {
                            print("🔕 [IAP] Listener: finishing renewal \(transaction.id)")
                            await transaction.finish()
                            continue
                        }
                    } else {
                        let age = Date().timeIntervalSince(transaction.purchaseDate)
                        if age > 60 {
                            print("🔕 [IAP] Listener: finishing old \(transaction.id) (age \(Int(age))s)")
                            await transaction.finish()
                            continue
                        }
                    }

                    print("⚠️ [IAP] Listener: unhandled \(transaction.id)")

                } catch {
                    print("⚠️ [IAP] Listener: verification failed: \(error)")
                }
            }
        }
    }

    // MARK: - Helpers

    private func checkiOSVersion() -> Bool {
        if #available(iOS 15.0, *) { return true }
        return false
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified: throw StoreError.failedVerification
        case .verified(let safe): return safe
        }
    }

    // MARK: - Initialize

    func initialize(callback: @escaping StoreKitInitCallback) {
        guard checkiOSVersion() else {
            callback(StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1))
            return
        }
        callback(StoreKitResult(success: true))
    }

    // MARK: - Get Products

    func getProducts(productIds: [String], callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            callback(StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1))
            return
        }
        Task {
            do {
                let storeProducts = try await Product.products(for: productIds)
                self.products = storeProducts
                let productArray = NSMutableArray()
                for product in storeProducts {
                    let priceAmountMicros = Int64((product.price as NSDecimalNumber).doubleValue * 1_000_000)
                    productArray.add(StoreKitProduct(
                        id: product.id,
                        displayName: product.displayName,
                        description: product.description,
                        price: product.displayPrice,
                        currencyCode: product.priceFormatStyle.currencyCode ?? "",
                        priceAmountMicros: priceAmountMicros,
                        type: product.type == .consumable || product.type == .nonConsumable ? "inapp" : "subs"
                    ))
                }
                callback(StoreKitResult(success: true, data: productArray))
            } catch {
                callback(StoreKitResult(success: false, errorMessage: error.localizedDescription, errorCode: -1))
            }
        }
    }

    // MARK: - Switch User

    func switchUser(newUserId: String?, callback: ((Bool) -> Void)? = nil) {
        let newUUID = newUserId.flatMap { UUID(uuidString: $0) }
        print("🔀 [IAP] switchUser → \(newUserId ?? "nil") → UUID: \(String(describing: newUUID))")
        currentUserId = newUUID
        handledTransactionIds.removeAll()

        Task {
            // Drain transactions that belong to other users now that currentUserId is set
            await drainStaleTransactionsAtLaunch()
            do {
                try await AppStore.sync()
                print("✅ [IAP] AppStore.sync() completed")
            } catch {
                print("⚠️ [IAP] AppStore.sync() failed: \(error)")
            }
            callback?(true)
        }
    }

    // MARK: - Launch Purchase Flow

    func launchPurchaseFlow(productId: String, userId: String?, callback: @escaping StoreKitPurchaseCallback) {
        guard checkiOSVersion() else {
            callback(StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1))
            return
        }
        guard let product = products.first(where: { $0.id == productId }) else {
            callback(StoreKitResult(success: false, errorMessage: "Product not found: \(productId)", errorCode: -1))
            return
        }

        // Use userId to build purchase option but do NOT override currentUserId here.
        // currentUserId should be set via switchUser() before calling this.
        let purchaseUUID: UUID? = userId.flatMap { UUID(uuidString: $0) }

        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        print("💳 [IAP] launchPurchaseFlow")
        print("💳 [IAP] productId        = \(productId)")
        print("💳 [IAP] userId (raw)     = \(userId ?? "nil")")
        print("💳 [IAP] purchaseUUID     = \(String(describing: purchaseUUID))")
        print("💳 [IAP] currentUserId    = \(String(describing: currentUserId))")
        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if purchaseUUID == nil && userId != nil {
            print("⚠️ [IAP] WARNING: userId '\(userId!)' could NOT be parsed as a UUID!")
        }

        Task {
            // Drain foreign transactions before purchase
            await drainStaleTransactionsAtLaunch()

            // Check if user already has an active entitlement for this product
            // from a different user. This prevents returning stale receipts.
            for await result in Transaction.currentEntitlements {
                if let t = try? checkVerified(result),
                   t.productID == productId {
                    // If the entitlement has a different appAccountToken, it belongs to another app user
                    if let currentUUID = currentUserId,
                       let entitlementToken = t.appAccountToken,
                       entitlementToken != currentUUID {
                        print("❌ [IAP] Existing entitlement for \(productId) belongs to different user (token=\(entitlementToken), current=\(currentUUID))")
                        callback(StoreKitResult(
                            success: false,
                            errorMessage: "Product already owned by a different account",
                            errorCode: 7 // ITEM_ALREADY_OWNED equivalent
                        ))
                        return
                    }
                }
            }

            await performPurchase(
                product: product,
                currentUserUUID: purchaseUUID,
                callback: callback
            )
        }
    }

    // MARK: - Perform Purchase
    //
    // Token mismatch: NO retry. Log everything and fail fast so we can
    // diagnose exactly what UUID is in the transaction vs what we sent.

    private func performPurchase(
        product: Product,
        currentUserUUID: UUID?,
        callback: @escaping StoreKitPurchaseCallback
    ) async {
        do {
            var options: Set<Product.PurchaseOption> = []
            if let uuid = currentUserUUID {
                options.insert(.appAccountToken(uuid))
                print("🔧 [IAP] Purchase option: appAccountToken = \(uuid)")
            } else {
                print("⚠️ [IAP] Purchase option: NO appAccountToken set (userId was nil or not a valid UUID)")
            }

            let purchaseResult = options.isEmpty
                ? try await product.purchase()
                : try await product.purchase(options: options)

            switch purchaseResult {
            case .success(let verification):
                let jwsRepresentation = verification.jwsRepresentation
                let transaction = try checkVerified(verification)

                print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                print("✅ [IAP] Transaction received from StoreKit")
                print("✅ [IAP] transaction.id              = \(transaction.id)")
                print("✅ [IAP] transaction.productID       = \(transaction.productID)")
                print("✅ [IAP] transaction.appAccountToken = \(String(describing: transaction.appAccountToken))")
                print("✅ [IAP] currentUserUUID             = \(String(describing: currentUserUUID))")
                print("✅ [IAP] transaction.purchaseDate    = \(transaction.purchaseDate)")
                print("✅ [IAP] transaction.revocationDate  = \(String(describing: transaction.revocationDate))")

                // Token mismatch check — NO retry, just log and decide
                if let currentUUID = currentUserUUID,
                   let transactionToken = transaction.appAccountToken {

                    let matches = transactionToken == currentUUID
                    print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    print("🔍 [IAP] TOKEN COMPARISON")
                    print("🔍 [IAP] currentUserUUID     = \(currentUUID)")
                    print("🔍 [IAP] transactionToken    = \(transactionToken)")
                    print("🔍 [IAP] tokens match        = \(matches)")
                    print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    if !matches {
                        // NO retry — finish this transaction and surface the error
                        // so we can see in logs exactly what UUIDs don't match
                        print("❌ [IAP] TOKEN MISMATCH — finishing transaction and failing")
                        print("❌ [IAP] This transaction belongs to a different app user")
                        print("❌ [IAP] Expected: \(currentUUID)")
                        print("❌ [IAP] Got:      \(transactionToken)")
                        await transaction.finish()
                        callback(StoreKitResult(
                            success: false,
                            errorMessage: "Token mismatch: expected \(currentUUID) got \(transactionToken)",
                            errorCode: -1
                        ))
                        return
                    }
                } else {
                    // Log why token check was skipped
                    print("ℹ️ [IAP] Token check SKIPPED")
                    print("ℹ️ [IAP] currentUserUUID         = \(String(describing: currentUserUUID))")
                    print("ℹ️ [IAP] transaction.appToken    = \(String(describing: transaction.appAccountToken))")
                    if currentUserUUID == nil {
                        print("ℹ️ [IAP] Reason: no currentUserUUID — userId was nil or not a valid UUID")
                    } else {
                        print("ℹ️ [IAP] Reason: transaction has no appAccountToken (legacy purchase)")
                    }
                }

                // Valid purchase — finish before notifying Kotlin
                handledTransactionIds.insert(transaction.id)
                await transaction.finish()
                print("✅ [IAP] Transaction \(transaction.id) finished successfully")

                purchaseUpdateCallback?(StoreKitPurchase(
                    productId: transaction.productID,
                    purchaseToken: String(transaction.id),
                    orderId: String(transaction.id),
                    purchaseTime: Int64(transaction.purchaseDate.timeIntervalSince1970 * 1000),
                    isAcknowledged: true,
                    originalJson: "",
                    signature: "",
                    transactionId: String(transaction.id),
                    jwsRepresentation: jwsRepresentation,
                    originalTransactionId: String(transaction.originalID)
                ))

                callback(StoreKitResult(success: true))

            case .userCancelled:
                print("🚫 [IAP] User cancelled purchase")
                callback(StoreKitResult(success: false, errorMessage: "User cancelled", errorCode: 2))
            case .pending:
                print("⏳ [IAP] Purchase pending")
                callback(StoreKitResult(success: false, errorMessage: "Purchase pending", errorCode: 3))
            @unknown default:
                callback(StoreKitResult(success: false, errorMessage: "Unknown result", errorCode: -1))
            }
        } catch {
            print("❌ [IAP] Purchase threw error: \(error)")
            callback(StoreKitResult(success: false, errorMessage: error.localizedDescription, errorCode: -1))
        }
    }

    // MARK: - Consume Purchase

    func consumePurchase(purchaseToken: String, callback: @escaping StoreKitPurchaseCallback) {
        Task {
            for await result in Transaction.currentEntitlements {
                if let t = try? checkVerified(result), String(t.id) == purchaseToken {
                    await t.finish(); break
                }
            }
            callback(StoreKitResult(success: true))
        }
    }

    // MARK: - Acknowledge Purchase

    func acknowledgePurchase(purchaseToken: String, callback: @escaping StoreKitPurchaseCallback) {
        Task {
            for await result in Transaction.currentEntitlements {
                if let t = try? checkVerified(result), String(t.id) == purchaseToken {
                    await t.finish(); break
                }
            }
            callback(StoreKitResult(success: true))
        }
    }

    // MARK: - Get Purchases

    func getPurchases(callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            callback(StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1))
            return
        }
        Task {
            let purchaseArray = NSMutableArray()
            for await result in Transaction.currentEntitlements {
                do {
                    let transaction = try checkVerified(result)
                    if let currentUUID = currentUserId,
                       let token = transaction.appAccountToken,
                       token != currentUUID { continue }
                    purchaseArray.add(StoreKitPurchase(
                        productId: transaction.productID,
                        purchaseToken: String(transaction.id),
                        orderId: String(transaction.id),
                        purchaseTime: Int64(transaction.purchaseDate.timeIntervalSince1970 * 1000),
                        isAcknowledged: true,
                        originalJson: "",
                        signature: "",
                        transactionId: String(transaction.id),
                        jwsRepresentation: result.jwsRepresentation,
                        originalTransactionId: String(transaction.originalID)
                    ))
                } catch {
                    print("⚠️ [IAP] getPurchases: verification failed: \(error)")
                }
            }
            callback(StoreKitResult(success: true, data: purchaseArray))
        }
    }

    // MARK: - Restore Purchases

    func restorePurchases(callback: @escaping StoreKitProductsCallback) {
        guard checkiOSVersion() else {
            callback(StoreKitResult(success: false, errorMessage: "StoreKit 2 requires iOS 15.0 or later", errorCode: -1))
            return
        }
        Task {
            do {
                try await AppStore.sync()
                getPurchases(callback: callback)
            } catch {
                callback(StoreKitResult(success: false, errorMessage: error.localizedDescription, errorCode: -1))
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
        handledTransactionIds.removeAll()
        currentUserId = nil
    }
}

// MARK: - Public @objc Wrapper

@available(iOS 15.0, *)
@objc public class StoreKitManagerWrapper: NSObject {

    private let manager = StoreKitManager.shared

    @objc public override init() { super.init() }

    @objc public func initialize(completion: @escaping (StoreKitResult) -> Void) {
        manager.initialize(callback: completion)
    }

    @objc public func getProducts(productIds: [String], completion: @escaping (StoreKitResult) -> Void) {
        manager.getProducts(productIds: productIds, callback: completion)
    }

    @objc public func launchPurchaseFlow(productId: String, userId: String?, completion: @escaping (StoreKitResult) -> Void) {
        manager.launchPurchaseFlow(productId: productId, userId: userId, callback: completion)
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

    @objc public func switchUser(newUserId: String?, completion: ((Bool) -> Void)? = nil) {
        manager.switchUser(newUserId: newUserId, callback: completion)
    }

    @objc public func disconnect() {
        manager.disconnect()
    }
}

// MARK: - Errors

enum StoreError: Error {
    case failedVerification
}