@file:OptIn(ExperimentalForeignApi::class)

package com.multiplatform.inAppPurchase

import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import com.multiplatform.inAppPurchase.model.ProductType
import com.multiplatform.inAppPurchase.model.Purchase
import com.multiplatform.storekit.StoreKitManagerWrapper
import com.multiplatform.storekit.StoreKitProduct
import com.multiplatform.storekit.StoreKitPurchase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class IAPManager actual constructor() {

    private var storeKitWrapper: StoreKitManagerWrapper? = null
    private var isInitialized = false

    /**
     * Initialize StoreKit 2
     */
    actual suspend fun initialize(): IAPResult<Unit> = suspendCancellableCoroutine { continuation ->
        println("🚀 [IAP] Starting StoreKit 2 initialization...")

        storeKitWrapper = StoreKitManagerWrapper()

        storeKitWrapper?.initializeWithCompletion { result ->
            if (result?.success() == true) {
                println("✅ [IAP] StoreKit 2 initialization completed successfully")
                isInitialized = true
                continuation.resume(IAPResult.Success(Unit))
            } else {
                val errorMessage = result?.errorMessage() ?: "Unknown error"
                println("❌ [IAP] StoreKit 2 initialization failed: $errorMessage")
                continuation.resume(IAPResult.Error(errorMessage, result?.errorCode()))
            }
        } ?: run {
            continuation.resume(IAPResult.Error("Failed to create StoreKitManagerWrapper"))
        }
    }

    /**
     * Query product details from App Store using StoreKit 2
     */
    actual suspend fun getProducts(
        productIds: List<String>,
        productType: ProductType
    ): IAPResult<List<Product>> = suspendCancellableCoroutine { continuation ->

        println("🛒 [IAP] Starting product query for IDs: $productIds")

        if (!isInitialized) {
            println("❌ [IAP] Cannot query products - StoreKit 2 not initialized")
            continuation.resume(IAPResult.Error("StoreKit 2 not initialized"))
            return@suspendCancellableCoroutine
        }

        storeKitWrapper?.getProductsWithProductIds(productIds) { result ->
            if (result?.success() == true) {
                try {
                    val products = mutableListOf<Product>()

                    result.data()?.let { data ->
                        val count = data.count()
                        println("📦 [IAP] Found $count products")

                        for (i in 0 until count) {
                            val item = data[i]
//                                val item = data.objectAtIndex(i.toULong())
                            if (item is StoreKitProduct) {
                                val product = Product(
                                    id = item.id(),
                                    title = item.displayName(),
                                    description = item.productDescription(),
                                    price = item.price(),
                                    priceCurrencyCode = item.currencyCode(),
                                    priceAmountMicros = item.priceAmountMicros(),
                                    type = item.type()
                                )
                                products.add(product)
                                println("✅ [IAP] Added product: ${item.id()}")
                            }
                        }
                    }

                    println("🎉 [IAP] Product query completed - returning ${products.size} products")
                    continuation.resume(IAPResult.Success(products))
                } catch (e: Exception) {
                    println("❌ [IAP] Error parsing products: ${e.message}")
                    continuation.resume(IAPResult.Error("Failed to parse products: ${e.message}"))
                }
            } else {
                val errorMessage = result?.errorMessage() ?: "Unknown error"
                println("❌ [IAP] Product query failed: $errorMessage")
                continuation.resume(IAPResult.Error(errorMessage, result?.errorCode()))
            }
        } ?: run {
            continuation.resume(IAPResult.Error("StoreKitManagerWrapper not initialized"))
        }
    }

    /**
     * Launch purchase flow for a product using StoreKit 2
     */
    actual suspend fun launchPurchaseFlow(
        product: Product,
        basePlanId: String?,
        userId: String?
    ): IAPResult<Unit> = suspendCancellableCoroutine { continuation ->

        println("💳 [IAP] Starting purchase flow for product: ${product.id}")

        if (!isInitialized) {
            println("❌ [IAP] Cannot launch purchase - StoreKit 2 not initialized")
            continuation.resume(IAPResult.Error("StoreKit 2 not initialized"))
            return@suspendCancellableCoroutine
        }

        storeKitWrapper?.launchPurchaseFlowWithProductId(product.id, userId) { result ->
            if (result?.success() == true) {
                println("✅ [IAP] Purchase flow completed successfully")
                continuation.resume(IAPResult.Success(Unit))
            } else {
                val errorMessage = result?.errorMessage() ?: "Unknown error"
                println("❌ [IAP] Purchase flow failed: $errorMessage")

                when (val errorCode = result?.errorCode()) {
                    2 -> continuation.resume(
                        IAPResult.Error(
                            "Purchase cancelled by user",
                            errorCode
                        )
                    )

                    3 -> continuation.resume(
                        IAPResult.Error(
                            "Purchase is pending approval",
                            errorCode
                        )
                    )

                    else -> continuation.resume(IAPResult.Error(errorMessage, errorCode))
                }
            }
        } ?: run {
            continuation.resume(IAPResult.Error("StoreKitManagerWrapper not initialized"))
        }
    }

    /**
     * Consume a purchase (automatic in StoreKit 2)
     */
    actual suspend fun consumePurchase(purchase: Purchase): IAPResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            println("🔄 [IAP] Consuming purchase: ${purchase.productId}")

            storeKitWrapper?.consumePurchaseWithPurchaseToken(purchase.purchaseToken) { result ->
                if (result?.success() == true) {
                    println("✅ [IAP] Purchase consumed successfully")
                    continuation.resume(IAPResult.Success(Unit))
                } else {
                    val errorMessage = result?.errorMessage() ?: "Unknown error"
                    println("❌ [IAP] Failed to consume purchase: $errorMessage")
                    continuation.resume(IAPResult.Error(errorMessage, result?.errorCode()))
                }
            } ?: run {
                continuation.resume(IAPResult.Error("StoreKitManagerWrapper not initialized"))
            }
        }

    /**
     * Acknowledge a purchase (automatic in StoreKit 2)
     */
    actual suspend fun acknowledgePurchase(purchase: Purchase): IAPResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            println("🔄 [IAP] Acknowledging purchase: ${purchase.productId}")

            storeKitWrapper?.acknowledgePurchaseWithPurchaseToken(purchase.purchaseToken) { result ->
                if (result?.success() == true) {
                    println("✅ [IAP] Purchase acknowledged successfully")
                    continuation.resume(IAPResult.Success(Unit))
                } else {
                    val errorMessage = result?.errorMessage() ?: "Unknown error"
                    println("❌ [IAP] Failed to acknowledge purchase: $errorMessage")
                    continuation.resume(IAPResult.Error(errorMessage, result?.errorCode()))
                }
            } ?: run {
                continuation.resume(IAPResult.Error("StoreKitManagerWrapper not initialized"))
            }
        }

    /**
     * Get current purchases using StoreKit 2
     */
    actual suspend fun getPurchases(
        productType: ProductType
    ): IAPResult<List<Purchase>> = suspendCancellableCoroutine { continuation ->

        println("📋 [IAP] Getting current purchases")

        if (!isInitialized) {
            continuation.resume(IAPResult.Error("StoreKit 2 not initialized"))
            return@suspendCancellableCoroutine
        }

        storeKitWrapper?.getPurchasesWithCompletion { result ->
            if (result?.success() == true) {
                try {
                    val purchases = mutableListOf<Purchase>()

                    result.data()?.let { data ->
                        val count = data.count()
                        println("📦 [IAP] Found $count purchases")

                        for (i in 0 until count) {
                            val item = data[i]
//                                val item = data.objectAtIndex(i.toULong())
                            if (item is StoreKitPurchase) {
                                val purchase = Purchase(
                                    productId = item.productId(),
                                    purchaseToken = item.purchaseToken(),
                                    orderId = item.orderId(),
                                    purchaseTime = item.purchaseTime(),
                                    isAcknowledged = item.isAcknowledged(),
                                    originalJson = item.originalJson(),
                                    signature = item.signature(),
                                    jwsRepresentation = item.jwsRepresentation(),
                                    originalTransactionId = item.originalTransactionId(),
                                    transactionId = item.transactionId()
                                )
                                purchases.add(purchase)
                                println("✅ [IAP] Added purchase: ${item.productId()}")
                            }
                        }
                    }

                    println("🎉 [IAP] Get purchases completed - returning ${purchases.size} purchases")
                    continuation.resume(IAPResult.Success(purchases))
                } catch (e: Exception) {
                    println("❌ [IAP] Error parsing purchases: ${e.message}")
                    continuation.resume(IAPResult.Error("Failed to parse purchases: ${e.message}"))
                }
            } else {
                val errorMessage = result?.errorMessage() ?: "Unknown error"
                println("❌ [IAP] Get purchases failed: $errorMessage")
                continuation.resume(IAPResult.Error(errorMessage, result?.errorCode()))
            }
        } ?: run {
            continuation.resume(IAPResult.Error("StoreKitManagerWrapper not initialized"))
        }
    }

    /**
     * Restore purchases using StoreKit 2
     */
    actual suspend fun restorePurchases(): IAPResult<List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            println("♻️ [IAP] Restoring purchases")

            if (!isInitialized) {
                continuation.resume(IAPResult.Error("StoreKit 2 not initialized"))
                return@suspendCancellableCoroutine
            }

            storeKitWrapper?.restorePurchasesWithCompletion { result ->
                if (result?.success() == true) {
                    try {
                        val purchases = mutableListOf<Purchase>()

                        result.data()?.let { data ->
                            val count = data.count()
                            println("📦 [IAP] Restored $count purchases")

                            for (i in 0 until count) {
                                val item = data[i]
//                                val item = data.objectAtIndex(i.toULong())
                                if (item is StoreKitPurchase) {
                                    val purchase = Purchase(
                                        productId = item.productId(),
                                        purchaseToken = item.purchaseToken(),
                                        orderId = item.orderId(),
                                        purchaseTime = item.purchaseTime(),
                                        isAcknowledged = item.isAcknowledged(),
                                        originalJson = item.originalJson(),
                                        signature = item.signature(),
                                        jwsRepresentation = item.jwsRepresentation(),
                                        originalTransactionId = item.originalTransactionId(),
                                        transactionId = item.transactionId()
                                    )
                                    purchases.add(purchase)
                                    println("✅ [IAP] Restored purchase: ${item.productId()}")
                                }
                            }
                        }

                        println("🎉 [IAP] Restore purchases completed - returning ${purchases.size} purchases")
                        continuation.resume(IAPResult.Success(purchases))
                    } catch (e: Exception) {
                        println("❌ [IAP] Error parsing restored purchases: ${e.message}")
                        continuation.resume(IAPResult.Error("Failed to parse restored purchases: ${e.message}"))
                    }
                } else {
                    val errorMessage = result?.errorMessage() ?: "Unknown error"
                    println("❌ [IAP] Restore purchases failed: $errorMessage")
                    continuation.resume(IAPResult.Error(errorMessage, result?.errorCode()))
                }
            } ?: run {
                continuation.resume(IAPResult.Error("StoreKitManagerWrapper not initialized"))
            }
        }

    /**
     * Flow of purchase updates using StoreKit 2
     */
    actual fun getPurchaseUpdates(): Flow<Purchase> = callbackFlow {
        println("📡 [IAP] Setting up purchase updates flow")

        storeKitWrapper?.setPurchaseUpdateCallbackWithCallback { purchase ->
            try {
                println("📦 [IAP] Processing purchase update for: ${purchase?.productId()}")

                val commonPurchase = Purchase(
                    productId = purchase?.productId() ?: "",
                    purchaseToken = purchase?.purchaseToken() ?: "",
                    orderId = purchase?.orderId() ?: "",
                    purchaseTime = purchase?.purchaseTime() ?: 0L,
                    isAcknowledged = purchase?.isAcknowledged() ?: false,
                    originalJson = purchase?.originalJson() ?: "",
                    signature = purchase?.signature() ?: "",
                    jwsRepresentation = purchase?.jwsRepresentation() ?: "",
                    originalTransactionId = purchase?.originalTransactionId() ?: "",
                    transactionId = purchase?.transactionId() ?: ""
                )

                println("✅ [IAP] Created Purchase object:")
                println("   Product ID: ${commonPurchase.productId}")
                println("   Purchase Token: ${commonPurchase.purchaseToken}")
                println("   Order ID: ${commonPurchase.orderId}")
                println("   Purchase Time: ${commonPurchase.purchaseTime}")
                println("   Is Acknowledged: ${commonPurchase.isAcknowledged}")

                val success = trySend(commonPurchase).isSuccess
                println("📤 [IAP] Sent purchase update to flow: $success")
            } catch (e: Exception) {
                println("❌ [IAP] Error processing purchase update: ${e.message}")
            }
        }

        awaitClose {
            println("🔌 [IAP] Closing purchase updates flow")
            storeKitWrapper?.setPurchaseUpdateCallbackWithCallback { _ -> }
        }
    }

    /**
     * Switch user context — call this on login/logout to set the current app user.
     * Drains stale transactions from other users and syncs with App Store.
     */
    actual suspend fun switchUser(userId: String?): Unit = suspendCancellableCoroutine { continuation ->
        println("🔀 [IAP] switchUser called with userId: $userId")

        storeKitWrapper?.switchUserWithNewUserId(userId) { _ ->
            println("✅ [IAP] switchUser completed")
            continuation.resume(Unit)
        } ?: run {
            println("⚠️ [IAP] switchUser: StoreKitManagerWrapper not initialized, continuing anyway")
            continuation.resume(Unit)
        }
    }

    /**
     * Disconnect and cleanup
     */
    actual suspend fun disconnect() {
        println("🔌 [IAP] Disconnecting StoreKit 2")
        storeKitWrapper?.disconnect()
        storeKitWrapper = null
        isInitialized = false
        println("✅ [IAP] StoreKit 2 disconnected")
    }
}
