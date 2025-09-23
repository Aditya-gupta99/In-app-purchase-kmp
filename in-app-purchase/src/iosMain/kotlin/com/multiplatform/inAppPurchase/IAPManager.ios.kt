// File: shared/src/iosMain/kotlin/com/multiplatform/inAppPurchase/IAPManager.ios.kt
package com.multiplatform.inAppPurchase

import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import com.multiplatform.inAppPurchase.model.Purchase
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.StoreKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class IAPManager actual constructor() {

    private var paymentQueue: SKPaymentQueue? = null
    private var productsRequest: SKProductsRequest? = null
    private var productDetailsCache = mutableMapOf<String, SKProduct>()
    private var isInitialized = false
    private var purchaseUpdatesCallback: ((Purchase) -> Unit)? = null

    // Keep strong references to delegates to prevent garbage collection
    private var currentProductsDelegate: ProductsRequestDelegate? = null

    // StoreKit delegate for handling products request
    private class ProductsRequestDelegate : NSObject(), SKProductsRequestDelegateProtocol {
        var onSuccess: ((SKProductsResponse) -> Unit)? = null
        var onError: ((NSError) -> Unit)? = null

        override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
            onSuccess?.invoke(didReceiveResponse)
        }

        override fun request(request: SKRequest, didFailWithError: NSError) {
            onError?.invoke(didFailWithError)
        }
    }

    // Payment transaction observer
    private class PaymentTransactionObserver : NSObject(), SKPaymentTransactionObserverProtocol {
        var purchaseUpdatesCallback: ((SKPaymentTransaction) -> Unit)? = null
        var restoreCompletedCallback: ((List<SKPaymentTransaction>) -> Unit)? = null
        var restoreFailedCallback: ((NSError) -> Unit)? = null

        private val restoredTransactions = mutableListOf<SKPaymentTransaction>()

        override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
            println("🔄 [IAP] Payment queue updated with ${updatedTransactions.size} transactions")

            updatedTransactions.forEach { transaction ->
                if (transaction is SKPaymentTransaction) {
                    val productId = transaction.payment.productIdentifier
                    val transactionId = transaction.transactionIdentifier
                    val state = transaction.transactionState

                    println("📋 [IAP] Processing transaction:")
                    println("   Product ID: $productId")
                    println("   Transaction ID: $transactionId")
                    println("   State: $state")

                    when (state) {
                        SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                            println("✅ [IAP] Transaction PURCHASED: $productId")
                            purchaseUpdatesCallback?.invoke(transaction)
                            println("🏁 [IAP] Finishing successful transaction: $transactionId")
                            queue.finishTransaction(transaction)
                        }
                        SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                            println("♻️ [IAP] Transaction RESTORED: $productId")
                            restoredTransactions.add(transaction)
                            purchaseUpdatesCallback?.invoke(transaction)
                        }
                        SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                            println("❌ [IAP] Transaction FAILED: $productId")
                            handleFailedTransaction(transaction, queue)
                        }
                        SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> {
                            println("🔄 [IAP] Transaction PURCHASING: $productId")
                        }
                        SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> {
                            println("⏳ [IAP] Transaction DEFERRED: $productId")
                        }
                        else -> {
                            println("❓ [IAP] Transaction UNKNOWN STATE ($state): $productId")
                        }
                    }
                } else {
                    println("⚠️ [IAP] Received non-SKPaymentTransaction object: $transaction")
                }
            }
        }

        override fun paymentQueue(queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError: NSError) {
            println("❌ [IAP] Restore transactions failed:")
            println("   Error: ${restoreCompletedTransactionsFailedWithError.localizedDescription}")
            println("   Code: ${restoreCompletedTransactionsFailedWithError.code}")

            restoreFailedCallback?.invoke(restoreCompletedTransactionsFailedWithError)
        }

        override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
            println("✅ [IAP] Restore transactions completed")
            println("   Restored ${restoredTransactions.size} transactions")

            restoreCompletedCallback?.invoke(restoredTransactions.toList())
            restoredTransactions.clear()
        }

        private fun handleFailedTransaction(transaction: SKPaymentTransaction, queue: SKPaymentQueue) {
            val error = transaction.error
            val productId = transaction.payment.productIdentifier
            val transactionId = transaction.transactionIdentifier

            println("❌ [IAP] Handling failed transaction:")
            println("   Product ID: $productId")
            println("   Transaction ID: $transactionId")

            if (error != null) {
                val nsError = error as NSError
                val errorCode = nsError.code
                val errorDomain = nsError.domain
                val errorDescription = nsError.localizedDescription

                println("   Error Code: $errorCode")
                println("   Error Domain: $errorDomain")
                println("   Error Description: $errorDescription")

                // Check if user cancelled
                if (errorCode == 2L) { // SKErrorPaymentCancelled
                    println("👤 [IAP] Purchase cancelled by user: $productId")
                } else {
                    println("💥 [IAP] Purchase failed with error: $productId - $errorDescription")
                }
            } else {
                println("❓ [IAP] Transaction failed but no error provided")
            }

            println("🏁 [IAP] Finishing failed transaction: $transactionId")
            queue.finishTransaction(transaction)
        }
    }

    private val paymentTransactionObserver = PaymentTransactionObserver()

    /**
     * Initialize StoreKit
     */
    actual suspend fun initialize(): IAPResult<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            println("🚀 [IAP] Starting StoreKit initialization...")

            if (!SKPaymentQueue.canMakePayments()) {
                println("❌ [IAP] In-app purchases are disabled on this device")
                continuation.resume(IAPResult.Error("In-app purchases are disabled"))
                return@suspendCancellableCoroutine
            }

            println("✅ [IAP] Device supports in-app purchases")

            paymentQueue = SKPaymentQueue.defaultQueue()
            paymentQueue?.addTransactionObserver(paymentTransactionObserver)

            println("✅ [IAP] Transaction observer added to payment queue")

            isInitialized = true
            println("🎉 [IAP] StoreKit initialization completed successfully")
            continuation.resume(IAPResult.Success(Unit))
        } catch (e: Exception) {
            println("❌ [IAP] StoreKit initialization failed: ${e.message}")
            continuation.resume(IAPResult.Error("Failed to initialize StoreKit: ${e.message}"))
        }
    }

    /**
     * Query product details from App Store
     */
    actual suspend fun getProducts(productIds: List<String>): IAPResult<List<Product>> =
        suspendCancellableCoroutine { continuation ->
            println("🛒 [IAP] Starting product query for IDs: $productIds")

            if (!isInitialized) {
                println("❌ [IAP] Cannot query products - StoreKit not initialized")
                continuation.resume(IAPResult.Error("StoreKit not initialized"))
                return@suspendCancellableCoroutine
            }

            try {
                // Create NSSet from productIds list
                val productIdentifiers = NSSet.setWithArray(productIds)
                println("🔄 [IAP] Created NSSet with ${productIdentifiers.count()} product identifiers")

                val request = SKProductsRequest(productIdentifiers)
                println("🔄 [IAP] Created SKProductsRequest")

                val delegate = ProductsRequestDelegate()
                delegate.onSuccess = { response ->
                    println("✅ [IAP] Products request completed successfully")

                    try {
                        val products = mutableListOf<Product>()

                        // Process valid products
                        val validProducts = response.products
                        val validCount = validProducts.count()
                        println("📦 [IAP] Found $validCount valid products")

                        for (i in 0 until validCount) {
                            val skProduct = validProducts[i] as? SKProduct
                            skProduct?.let { product ->
                                println("📦 [IAP] Processing product: ${product.productIdentifier}")
                                println("   Title: ${product.localizedTitle}")
                                println("   Description: ${product.localizedDescription}")
                                println("   Price: ${product.price}")
                                println("   Currency: ${product.priceLocale.currencyCode}")

                                // Cache for later purchase
                                productDetailsCache[product.productIdentifier] = product

                                // Create formatter for price
                                val formatter = NSNumberFormatter().apply {
                                    numberStyle = NSNumberFormatterCurrencyStyle
                                    locale = product.priceLocale
                                }

                                val formattedPrice = formatter.stringFromNumber(product.price) ?: product.price.stringValue
                                println("   Formatted Price: $formattedPrice")

                                val productItem = Product(
                                    id = product.productIdentifier,
                                    title = product.localizedTitle,
                                    description = product.localizedDescription,
                                    price = formattedPrice,
                                    priceCurrencyCode = product.priceLocale.currencyCode ?: "",
                                    priceAmountMicros = (product.price.doubleValue * 1_000_000).toLong(),
                                    type = "inapp"
                                )
                                products.add(productItem)
                                println("✅ [IAP] Added product to results: ${product.productIdentifier}")
                            }
                        }

                        // Log invalid product IDs for debugging
                        val invalidProductIds = response.invalidProductIdentifiers
                        val invalidCount = invalidProductIds.count()

                        if (invalidCount > 0) {
                            println("⚠️ [IAP] Found $invalidCount invalid product IDs:")
                            for (i in 0 until invalidCount) {
                                val invalidId = invalidProductIds[i] as? String
                                invalidId?.let {
                                    println("   ❌ Invalid: $it")
                                }
                            }
                        } else {
                            println("✅ [IAP] All product IDs are valid")
                        }

                        println("🎉 [IAP] Product query completed - returning ${products.size} products")
                        continuation.resume(IAPResult.Success(products))
                        currentProductsDelegate = null
                    } catch (e: Exception) {
                        println("❌ [IAP] Error parsing products: ${e.message}")
                        continuation.resume(IAPResult.Error("Failed to parse products: ${e.message}"))
                        currentProductsDelegate = null
                    }
                }

                delegate.onError = { error ->
                    println("❌ [IAP] Products request failed:")
                    println("   Error: ${error.localizedDescription}")
                    println("   Code: ${error.code}")
                    println("   Domain: ${error.domain}")

                    continuation.resume(
                        IAPResult.Error(
                            "Failed to query products: ${error.localizedDescription}",
                            error.code.toInt()
                        )
                    )
                    currentProductsDelegate = null
                }

                // Keep strong reference to prevent garbage collection
                currentProductsDelegate = delegate
                request.delegate = delegate

                println("🚀 [IAP] Starting products request...")
                request.start()

                // Store reference to avoid garbage collection
                productsRequest = request

            } catch (e: Exception) {
                println("❌ [IAP] Failed to create product request: ${e.message}")
                continuation.resume(IAPResult.Error("Failed to create product request: ${e.message}"))
            }
        }

    /**
     * Launch purchase flow for a product
     */
    actual suspend fun launchPurchaseFlow(product: Product): IAPResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            println("💳 [IAP] Starting purchase flow for product: ${product.id}")

            if (!isInitialized) {
                println("❌ [IAP] Cannot launch purchase - StoreKit not initialized")
                continuation.resume(IAPResult.Error("StoreKit not initialized"))
                return@suspendCancellableCoroutine
            }

            val skProduct = productDetailsCache[product.id]
            if (skProduct == null) {
                println("❌ [IAP] Product not found in cache: ${product.id}")
                println("   Available products in cache: ${productDetailsCache.keys}")
                continuation.resume(IAPResult.Error("Product not found: ${product.id}. Call getProducts first."))
                return@suspendCancellableCoroutine
            }

            try {
                println("🔄 [IAP] Creating payment for product: ${skProduct.productIdentifier}")
                println("   Product title: ${skProduct.localizedTitle}")
                println("   Product price: ${skProduct.price}")

                val payment = SKPayment.paymentWithProduct(skProduct)
                println("✅ [IAP] Payment object created successfully")

                println("🚀 [IAP] Adding payment to queue...")
                paymentQueue?.addPayment(payment)

                // The actual purchase result will be delivered via the transaction observer
                println("✅ [IAP] Payment added to queue - waiting for transaction updates")
                continuation.resume(IAPResult.Success(Unit))
            } catch (e: Exception) {
                println("❌ [IAP] Failed to launch purchase flow: ${e.message}")
                continuation.resume(IAPResult.Error("Failed to launch purchase flow: ${e.message}"))
            }
        }

    /**
     * Consume a purchase (finish transaction for iOS)
     */
    actual suspend fun consumePurchase(purchase: Purchase): IAPResult<Unit> {
        // For iOS, consuming is handled automatically when transaction is finished
        return IAPResult.Success(Unit)
    }

    /**
     * Acknowledge a purchase (finish transaction for iOS)
     */
    actual suspend fun acknowledgePurchase(purchase: Purchase): IAPResult<Unit> {
        // Similar to consume - in iOS you finish the transaction
        // The transaction finishing should happen in the transaction observer
        return IAPResult.Success(Unit)
    }

    /**
     * Get current purchases (restored transactions)
     */
    actual suspend fun getPurchases(): IAPResult<List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            if (!isInitialized) {
                continuation.resume(IAPResult.Error("StoreKit not initialized"))
                return@suspendCancellableCoroutine
            }

            val purchases = mutableListOf<Purchase>()
            var isCompleted = false

            val observer = object : NSObject(), SKPaymentTransactionObserverProtocol {
                override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
                    updatedTransactions.forEach { transaction ->
                        if (transaction is SKPaymentTransaction) {
                            when (transaction.transactionState) {
                                SKPaymentTransactionState.SKPaymentTransactionStateRestored,
                                SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                                    val purchase = transaction.toCommonPurchase()
                                    purchases.add(purchase)
                                }
                                else -> {}
                            }
                        }
                    }
                }

                override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
                    if (!isCompleted) {
                        isCompleted = true
                        queue.removeTransactionObserver(this)
                        continuation.resume(IAPResult.Success(purchases.toList()))
                    }
                }

                override fun paymentQueue(queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError: NSError) {
                    if (!isCompleted) {
                        isCompleted = true
                        queue.removeTransactionObserver(this)
                        continuation.resume(
                            IAPResult.Error(
                                "Failed to get purchases: ${restoreCompletedTransactionsFailedWithError.localizedDescription}",
                                restoreCompletedTransactionsFailedWithError.code.toInt()
                            )
                        )
                    }
                }
            }

            paymentQueue?.addTransactionObserver(observer)
            paymentQueue?.restoreCompletedTransactions()
        }

    /**
     * Restore purchases
     */
    actual suspend fun restorePurchases(): IAPResult<List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            if (!isInitialized) {
                continuation.resume(IAPResult.Error("StoreKit not initialized"))
                return@suspendCancellableCoroutine
            }

            val restoredPurchases = mutableListOf<Purchase>()

            paymentTransactionObserver.restoreCompletedCallback = { transactions ->
                transactions.forEach { transaction ->
                    val purchase = transaction.toCommonPurchase()
                    restoredPurchases.add(purchase)
                    // Finish the restored transaction
                    paymentQueue?.finishTransaction(transaction)
                }
                continuation.resume(IAPResult.Success(restoredPurchases.toList()))

                // Clean up callbacks
                paymentTransactionObserver.restoreCompletedCallback = null
                paymentTransactionObserver.restoreFailedCallback = null
            }

            paymentTransactionObserver.restoreFailedCallback = { error ->
                continuation.resume(
                    IAPResult.Error(
                        "Failed to restore purchases: ${error.localizedDescription}",
                        error.code.toInt()
                    )
                )

                // Clean up callbacks
                paymentTransactionObserver.restoreCompletedCallback = null
                paymentTransactionObserver.restoreFailedCallback = null
            }

            paymentQueue?.restoreCompletedTransactions()
        }

    /**
     * Flow of purchase updates
     */
    actual fun getPurchaseUpdates(): Flow<Purchase> = callbackFlow {
        println("📡 [IAP] Setting up purchase updates flow")

        val originalCallback = paymentTransactionObserver.purchaseUpdatesCallback

        paymentTransactionObserver.purchaseUpdatesCallback = { transaction ->
            try {
                println("📦 [IAP] Processing purchase update for: ${transaction.payment.productIdentifier}")
                val purchase = transaction.toCommonPurchase()

                println("✅ [IAP] Created Purchase object:")
                println("   Product ID: ${purchase.productId}")
                println("   Purchase Token: ${purchase.purchaseToken}")
                println("   Order ID: ${purchase.orderId}")
                println("   Purchase Time: ${purchase.purchaseTime}")
                println("   Is Acknowledged: ${purchase.isAcknowledged}")

                val success = trySend(purchase).isSuccess
                println("📤 [IAP] Sent purchase update to flow: $success")
            } catch (e: Exception) {
                println("❌ [IAP] Error processing purchase update: ${e.message}")
            }
        }

        awaitClose {
            println("🔌 [IAP] Closing purchase updates flow")
            paymentTransactionObserver.purchaseUpdatesCallback = originalCallback
        }
    }

    /**
     * Disconnect and cleanup
     */
    actual suspend fun disconnect() {
        paymentQueue?.removeTransactionObserver(paymentTransactionObserver)
        paymentQueue = null
        productsRequest = null
        productDetailsCache.clear()
        paymentTransactionObserver.purchaseUpdatesCallback = null
        paymentTransactionObserver.restoreCompletedCallback = null
        paymentTransactionObserver.restoreFailedCallback = null
        currentProductsDelegate = null
        isInitialized = false
    }

    /**
     * Helper function to convert SKPaymentTransaction to common Purchase model
     */
    private fun SKPaymentTransaction.toCommonPurchase(): Purchase {
        return Purchase(
            productId = this.payment.productIdentifier,
            purchaseToken = this.transactionIdentifier ?: "",
            orderId = this.transactionIdentifier ?: "",
            purchaseTime = (this.transactionDate?.timeIntervalSince1970?.toLong() ?: 0) * 1000,
            isAcknowledged = true, // iOS doesn't have acknowledged state like Android
            originalJson = "", // iOS doesn't provide original JSON like Android
            signature = "" // iOS doesn't provide signature like Android
        )
    }
}