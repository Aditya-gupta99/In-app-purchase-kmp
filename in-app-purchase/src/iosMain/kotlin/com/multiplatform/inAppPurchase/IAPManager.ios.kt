package com.multiplatform.inAppPurchase

import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import com.multiplatform.inAppPurchase.model.Purchase
import com.multiplatform.storekit.*
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSArray
import platform.Foundation.NSMutableArray
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class IAPManager actual constructor() {

    private var purchaseUpdatesCallback: ((Purchase) -> Unit)? = null
    private var isInitialized = false

    /**
     * Initialize StoreKit 2
     */
    actual suspend fun initialize(): IAPResult<Unit> = suspendCancellableCoroutine { continuation ->
        println("🚀 [IAP] Starting StoreKit 2 initialization...")

        val callback: StoreKitInitCallback = staticCFunction { result ->
            val manager = StableRef.create(continuation).asCPointer()
            val cont = manager.asStableRef<CancellableContinuation<IAPResult<Unit>>>().get()

            if (result.success) {
                println("✅ [IAP] StoreKit 2 initialization completed successfully")
                cont.resume(IAPResult.Success(Unit))
            } else {
                val errorMessage = result.errorMessage ?: "Unknown error"
                println("❌ [IAP] StoreKit 2 initialization failed: $errorMessage")
                cont.resume(IAPResult.Error(errorMessage, result.errorCode))
            }

            manager.asStableRef<CancellableContinuation<IAPResult<Unit>>>().dispose()
        }

        try {
            storekit_initialize(callback)
            isInitialized = true
        } catch (e: Exception) {
            println("❌ [IAP] Failed to initialize StoreKit 2: ${e.message}")
            continuation.resume(IAPResult.Error("Failed to initialize StoreKit 2: ${e.message}"))
        }
    }

    /**
     * Query product details from App Store using StoreKit 2
     */
    actual suspend fun getProducts(productIds: List<String>): IAPResult<List<Product>> =
        suspendCancellableCoroutine { continuation ->
            println("🛒 [IAP] Starting product query for IDs: $productIds")

            if (!isInitialized) {
                println("❌ [IAP] Cannot query products - StoreKit 2 not initialized")
                continuation.resume(IAPResult.Error("StoreKit 2 not initialized"))
                return@suspendCancellableCoroutine
            }

            val callback: StoreKitProductsCallback = staticCFunction { result ->
                val manager = StableRef.create(continuation).asCPointer()
                val cont = manager.asStableRef<CancellableContinuation<IAPResult<List<Product>>>>().get()

                if (result.success) {
                    try {
                        val products = mutableListOf<Product>()

                        result.data?.let { data ->
                            if (data is NSArray) {
                                val count = data.count()
                                println("📦 [IAP] Found ${count} products")

                                for (i in 0 until count) {
                                    val item = data.objectAtIndex(i.toULong())
                                    if (item is StoreKitProduct) {
                                        val product = Product(
                                            id = item.id,
                                            title = item.displayName,
                                            description = item.description,
                                            price = item.price,
                                            priceCurrencyCode = item.currencyCode,
                                            priceAmountMicros = item.priceAmountMicros,
                                            type = item.type
                                        )
                                        products.add(product)
                                        println("✅ [IAP] Added product: ${item.id}")
                                    }
                                }
                            }
                        }

                        println("🎉 [IAP] Product query completed - returning ${products.size} products")
                        cont.resume(IAPResult.Success(products))
                    } catch (e: Exception) {
                        println("❌ [IAP] Error parsing products: ${e.message}")
                        cont.resume(IAPResult.Error("Failed to parse products: ${e.message}"))
                    }
                } else {
                    val errorMessage = result.errorMessage ?: "Unknown error"
                    println("❌ [IAP] Product query failed: $errorMessage")
                    cont.resume(IAPResult.Error(errorMessage, result.errorCode))
                }

                manager.asStableRef<CancellableContinuation<IAPResult<List<Product>>>>().dispose()
            }

            // Convert Kotlin strings to C strings
            memScoped {
                val cStringArray = allocArray<CPointerVar<ByteVar>>(productIds.size)
                productIds.forEachIndexed { index, productId ->
                    cStringArray[index] = productId.cstr.ptr
                }

                storekit_get_products(cStringArray, productIds.size, callback)
            }
        }

    /**
     * Launch purchase flow for a product using StoreKit 2
     */
    actual suspend fun launchPurchaseFlow(product: Product): IAPResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            println("💳 [IAP] Starting purchase flow for product: ${product.id}")

            if (!isInitialized) {
                println("❌ [IAP] Cannot launch purchase - StoreKit 2 not initialized")
                continuation.resume(IAPResult.Error("StoreKit 2 not initialized"))
                return@suspendCancellableCoroutine
            }

            val callback: StoreKitPurchaseCallback = staticCFunction { result ->
                val manager = StableRef.create(continuation).asCPointer()
                val cont = manager.asStableRef<CancellableContinuation<IAPResult<Unit>>>().get()

                if (result.success) {
                    println("✅ [IAP] Purchase flow completed successfully")
                    cont.resume(IAPResult.Success(Unit))
                } else {
                    val errorMessage = result.errorMessage ?: "Unknown error"
                    println("❌ [IAP] Purchase flow failed: $errorMessage")

                    // Handle specific error codes
                    val errorCode = result.errorCode
                    when (errorCode) {
                        2 -> cont.resume(IAPResult.Error("Purchase cancelled by user", errorCode))
                        3 -> cont.resume(IAPResult.Error("Purchase is pending approval", errorCode))
                        else -> cont.resume(IAPResult.Error(errorMessage, errorCode))
                    }
                }

                manager.asStableRef<CancellableContinuation<IAPResult<Unit>>>().dispose()
            }

            product.id.cstr.getPointer(memScope).let { productIdPtr ->
                storekit_launch_purchase_flow(productIdPtr, callback)
            }
        }

    /**
     * Consume a purchase (automatic in StoreKit 2)
     */
    actual suspend fun consumePurchase(purchase: Purchase): IAPResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            println("🔄 [IAP] Consuming purchase: ${purchase.productId}")

            val callback: StoreKitPurchaseCallback = staticCFunction { result ->
                val manager = StableRef.create(continuation).asCPointer()
                val cont = manager.asStableRef<CancellableContinuation<IAPResult<Unit>>>().get()

                if (result.success) {
                    println("✅ [IAP] Purchase consumed successfully")
                    cont.resume(IAPResult.Success(Unit))
                } else {
                    val errorMessage = result.errorMessage ?: "Unknown error"
                    println("❌ [IAP] Failed to consume purchase: $errorMessage")
                    cont.resume(IAPResult.Error(errorMessage, result.errorCode))
                }

                manager.asStableRef<CancellableContinuation<IAPResult<Unit>>>().dispose()
            }

            purchase.purchaseToken.cstr.getPointer(memScope).let { tokenPtr ->
                storekit_consume_purchase(tokenPtr, callback)
            }
        }

    /**
     * Acknowledge a purchase (automatic in StoreKit 2)
     */
    actual suspend fun acknowledgePurchase(purchase: Purchase): IAPResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            println("🔄 [IAP] Acknowledging purchase: ${purchase.productId}")

            val callback: StoreKitPurchaseCallback = staticCFunction { result ->
                val manager = StableRef.create(continuation).asCPointer()
                val cont = manager.asStableRef<CancellableContinuation<IAPResult<Unit>>>().get()

                if (result.success) {
                    println("✅ [IAP] Purchase acknowledged successfully")
                    cont.resume(IAPResult.Success(Unit))
                } else {
                    val errorMessage = result.errorMessage ?: "Unknown error"
                    println("❌ [IAP] Failed to acknowledge purchase: $errorMessage")
                    cont.resume(IAPResult.Error(errorMessage, result.errorCode))
                }

                manager.asStableRef<CancellableContinuation<IAPResult<Unit>>>().dispose()
            }

            purchase.purchaseToken.cstr.getPointer(memScope).let { tokenPtr ->
                storekit_acknowledge_purchase(tokenPtr, callback)
            }
        }

    /**
     * Get current purchases using StoreKit 2
     */
    actual suspend fun getPurchases(): IAPResult<List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            println("📋 [IAP] Getting current purchases")

            if (!isInitialized) {
                continuation.resume(IAPResult.Error("StoreKit 2 not initialized"))
                return@suspendCancellableCoroutine
            }

            val callback: StoreKitProductsCallback = staticCFunction { result ->
                val manager = StableRef.create(continuation).asCPointer()
                val cont = manager.asStableRef<CancellableContinuation<IAPResult<List<Purchase>>>>().get()

                if (result.success) {
                    try {
                        val purchases = mutableListOf<Purchase>()

                        result.data?.let { data ->
                            if (data is NSArray) {
                                val count = data.count()
                                println("📦 [IAP] Found ${count} purchases")

                                for (i in 0 until count) {
                                    val item = data.objectAtIndex(i.toULong())
                                    if (item is StoreKitPurchase) {
                                        val purchase = Purchase(
                                            productId = item.productId,
                                            purchaseToken = item.purchaseToken,
                                            orderId = item.orderId,
                                            purchaseTime = item.purchaseTime,
                                            isAcknowledged = item.isAcknowledged,
                                            originalJson = item.originalJson,
                                            signature = item.signature
                                        )
                                        purchases.add(purchase)
                                        println("✅ [IAP] Added purchase: ${item.productId}")
                                    }
                                }
                            }
                        }

                        println("🎉 [IAP] Get purchases completed - returning ${purchases.size} purchases")
                        cont.resume(IAPResult.Success(purchases))
                    } catch (e: Exception) {
                        println("❌ [IAP] Error parsing purchases: ${e.message}")
                        cont.resume(IAPResult.Error("Failed to parse purchases: ${e.message}"))
                    }
                } else {
                    val errorMessage = result.errorMessage ?: "Unknown error"
                    println("❌ [IAP] Get purchases failed: $errorMessage")
                    cont.resume(IAPResult.Error(errorMessage, result.errorCode))
                }

                manager.asStableRef<CancellableContinuation<IAPResult<List<Purchase>>>>().dispose()
            }

            storekit_get_purchases(callback)
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

            val callback: StoreKitProductsCallback = staticCFunction { result ->
                val manager = StableRef.create(continuation).asCPointer()
                val cont = manager.asStableRef<CancellableContinuation<IAPResult<List<Purchase>>>>().get()

                if (result.success) {
                    try {
                        val purchases = mutableListOf<Purchase>()

                        result.data?.let { data ->
                            if (data is NSArray) {
                                val count = data.count()
                                println("📦 [IAP] Restored ${count} purchases")

                                for (i in 0 until count) {
                                    val item = data.objectAtIndex(i.toULong())
                                    if (item is StoreKitPurchase) {
                                        val purchase = Purchase(
                                            productId = item.productId,
                                            purchaseToken = item.purchaseToken,
                                            orderId = item.orderId,
                                            purchaseTime = item.purchaseTime,
                                            isAcknowledged = item.isAcknowledged,
                                            originalJson = item.originalJson,
                                            signature = item.signature
                                        )
                                        purchases.add(purchase)
                                        println("✅ [IAP] Restored purchase: ${item.productId}")
                                    }
                                }
                            }
                        }

                        println("🎉 [IAP] Restore purchases completed - returning ${purchases.size} purchases")
                        cont.resume(IAPResult.Success(purchases))
                    } catch (e: Exception) {
                        println("❌ [IAP] Error parsing restored purchases: ${e.message}")
                        cont.resume(IAPResult.Error("Failed to parse restored purchases: ${e.message}"))
                    }
                } else {
                    val errorMessage = result.errorMessage ?: "Unknown error"
                    println("❌ [IAP] Restore purchases failed: $errorMessage")
                    cont.resume(IAPResult.Error(errorMessage, result.errorCode))
                }

                manager.asStableRef<CancellableContinuation<IAPResult<List<Purchase>>>>().dispose()
            }

            storekit_restore_purchases(callback)
        }

    /**
     * Flow of purchase updates using StoreKit 2
     */
    actual fun getPurchaseUpdates(): Flow<Purchase> = callbackFlow {
        println("📡 [IAP] Setting up purchase updates flow")

        val callback: StoreKitPurchaseUpdateCallback = staticCFunction { purchase ->
            val channelRef = StableRef.create(this@callbackFlow).asCPointer()
            val channel = channelRef.asStableRef<ProducerScope<Purchase>>().get()

            try {
                println("📦 [IAP] Processing purchase update for: ${purchase.productId}")

                val commonPurchase = Purchase(
                    productId = purchase.productId,
                    purchaseToken = purchase.purchaseToken,
                    orderId = purchase.orderId,
                    purchaseTime = purchase.purchaseTime,
                    isAcknowledged = purchase.isAcknowledged,
                    originalJson = purchase.originalJson,
                    signature = purchase.signature
                )

                println("✅ [IAP] Created Purchase object:")
                println("   Product ID: ${commonPurchase.productId}")
                println("   Purchase Token: ${commonPurchase.purchaseToken}")
                println("   Order ID: ${commonPurchase.orderId}")
                println("   Purchase Time: ${commonPurchase.purchaseTime}")
                println("   Is Acknowledged: ${commonPurchase.isAcknowledged}")

                val success = channel.trySend(commonPurchase).isSuccess
                println("📤 [IAP] Sent purchase update to flow: $success")
            } catch (e: Exception) {
                println("❌ [IAP] Error processing purchase update: ${e.message}")
            }

            // Don't dispose here as callback might be called multiple times
        }

        // Set the callback
        storekit_set_purchase_update_callback(callback)

        awaitClose {
            println("🔌 [IAP] Closing purchase updates flow")
            // Clear the callback by setting it to null equivalent
            storekit_set_purchase_update_callback(staticCFunction { _ -> })
        }
    }

    /**
     * Disconnect and cleanup
     */
    actual suspend fun disconnect() {
        println("🔌 [IAP] Disconnecting StoreKit 2")
        storekit_disconnect()
        purchaseUpdatesCallback = null
        isInitialized = false
        println("✅ [IAP] StoreKit 2 disconnected")
    }
}