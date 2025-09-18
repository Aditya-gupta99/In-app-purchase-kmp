package com.multiplatform.inAppPurchase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import com.multiplatform.inAppPurchase.model.Purchase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class IAPManager actual constructor() {

    private var billingClient: BillingClient? = null
    private var context: Context? = null
    private var activity: Activity? = null

    private var purchasesUpdatedCallback: ((com.android.billingclient.api.Purchase) -> Unit)? = null

    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    fun setContext(context: Context, activity: Activity? = null) {
        this.context = context.applicationContext
        this.activity = activity
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { p ->
                // Forward to a callback if set (used by the callbackFlow)
                purchasesUpdatedCallback?.invoke(p)
            }
        } else {
            // You can also expose errors via flows/callbacks if you want
        }
    }

    /**
     * Initialize BillingClient
     */
    actual suspend fun initialize(): IAPResult<Unit> = suspendCancellableCoroutine { continuation ->
        val context = this.context ?: run {
            continuation.resume(IAPResult.Error("Context not set"))
            return@suspendCancellableCoroutine
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(IAPResult.Success(Unit))
                } else {
                    continuation.resume(
                        IAPResult.Error(
                            "Billing setup failed: ${billingResult.debugMessage}",
                            billingResult.responseCode
                        )
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                continuation.resume(IAPResult.Error("Billing service disconnected"))
            }
        })
    }

    /**
     * Query product details (one-time products / subscriptions)
     */
    actual suspend fun getProducts(productIds: List<String>): IAPResult<List<Product>> =
        suspendCancellableCoroutine { continuation ->

            val client = billingClient ?: run {
                continuation.resume(IAPResult.Error("Billing client not initialized"))
                return@suspendCancellableCoroutine
            }

            val productList = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    // When building product list, choose ProductType.ONE_TIME_PRODUCT for v8 semantics or INAPP constant
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            client.queryProductDetailsAsync(params) { billingResult, productDetailsResponse ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val mapped = productDetailsResponse.productDetailsList.map {
                        // cache for later purchase
                        productDetailsCache[it.productId] = it
                        Product(
                            id = it.productId,
                            title = it.title,
                            description = it.description,
                            price = it.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
                            priceCurrencyCode = it.oneTimePurchaseOfferDetails?.priceCurrencyCode
                                ?: "",
                            priceAmountMicros = it.oneTimePurchaseOfferDetails?.priceAmountMicros
                                ?: 0,
                            type = it.productType
                        )
                    }
                    continuation.resume(IAPResult.Success(mapped))
                } else {
                    continuation.resume(
                        IAPResult.Error(
                            "Failed to query products: ${billingResult.debugMessage}",
                            billingResult.responseCode
                        )
                    )
                }
            }
        }

    /**
     * Launch purchase flow for a product.
     * Requires the Activity to be set (passed via setContext or argument).
     *
     * IMPORTANT: ProductDetails is the object returned by queryProductDetailsAsync.
     * You should call this from UI (Activity) scope because it needs an Activity.
     */
    actual suspend fun launchPurchaseFlow(
        product: Product
    ): IAPResult<Unit> = suspendCancellableCoroutine { continuation ->

        val client = billingClient ?: run {
            continuation.resume(IAPResult.Error("Billing client not initialized"))
            return@suspendCancellableCoroutine
        }
        val act = activity ?: run {
            continuation.resume(IAPResult.Error("Activity not set"))
            return@suspendCancellableCoroutine
        }

        val productDetails = productDetailsCache[product.id]
        if (productDetails == null) {
            continuation.resume(IAPResult.Error("ProductDetails not found for ${product.id}"))
            return@suspendCancellableCoroutine
        }


        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        // If an offer token is required, set it:
        productDetails.oneTimePurchaseOfferDetails?.offerToken?.let {
            if (it.isNotEmpty()) productParams.setOfferToken(it)
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams.build()))
            .build()

        val result = client.launchBillingFlow(act, billingFlowParams)

        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            continuation.resume(IAPResult.Success(Unit))
        } else {
            continuation.resume(
                IAPResult.Error(
                    "Failed to launch billing flow: ${result.debugMessage}",
                    result.responseCode
                )
            )
        }
    }

    /**
     * Consume a one-time product (in case you sell consumables).
     */
    actual suspend fun consumePurchase(purchase: Purchase): IAPResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            val client = billingClient ?: run {
                continuation.resume(IAPResult.Error("Billing client not initialized"))
                return@suspendCancellableCoroutine
            }

            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            client.consumeAsync(consumeParams) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(IAPResult.Success(Unit))
                } else {
                    continuation.resume(
                        IAPResult.Error(
                            "Failed to consume purchase: ${billingResult.debugMessage}",
                            billingResult.responseCode
                        )
                    )
                }
            }
        }

    /**
     * Acknowledge a purchase (non-consumable / managed one-time product or subscription).
     */
    actual suspend fun acknowledgePurchase(purchase: Purchase): IAPResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            val client = billingClient ?: run {
                continuation.resume(IAPResult.Error("Billing client not initialized"))
                return@suspendCancellableCoroutine
            }

            if (purchase.isAcknowledged) {
                continuation.resume(IAPResult.Success(Unit))
                return@suspendCancellableCoroutine
            }

            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            client.acknowledgePurchase(acknowledgeParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(IAPResult.Success(Unit))
                } else {
                    continuation.resume(
                        IAPResult.Error(
                            "Failed to acknowledge purchase: ${billingResult.debugMessage}",
                            billingResult.responseCode
                        )
                    )
                }
            }
        }

    /**
     * Query current owned purchases.
     */
    actual suspend fun getPurchases(): IAPResult<List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            val client = billingClient ?: run {
                continuation.resume(IAPResult.Error("Billing client not initialized"))
                return@suspendCancellableCoroutine
            }

            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            client.queryPurchasesAsync(params) { billingResult, purchasesResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val purchases = purchasesResult.map { p ->
                        Purchase(
                            productId = p.products.firstOrNull() ?: "",
                            purchaseToken = p.purchaseToken,
                            orderId = p.orderId ?: "",
                            purchaseTime = p.purchaseTime,
                            isAcknowledged = p.isAcknowledged,
                            originalJson = p.originalJson,
                            signature = p.signature
                        )
                    }
                    continuation.resume(IAPResult.Success(purchases))
                } else {
                    continuation.resume(
                        IAPResult.Error(
                            "Failed to query purchases: ${billingResult.debugMessage}",
                            billingResult.responseCode
                        )
                    )
                }
            }
        }

    /**
     * Restore == query purchases for Android
     */
    actual suspend fun restorePurchases(): IAPResult<List<Purchase>> {
        return getPurchases()
    }

    /**
     * Flow of purchase updates. Emits each platform Purchase as it arrives via PurchasesUpdatedListener.
     * The Purchase objects include originalJson + signature (useful for server verification).
     */
    actual fun getPurchaseUpdates(): Flow<Purchase> = callbackFlow {
        // install callback that will send purchases to the flow
        purchasesUpdatedCallback = { platformPurchase ->
            try {
                val mapped = Purchase(
                    productId = platformPurchase.products.firstOrNull() ?: "",
                    purchaseToken = platformPurchase.purchaseToken,
                    orderId = platformPurchase.orderId ?: "",
                    purchaseTime = platformPurchase.purchaseTime,
                    isAcknowledged = platformPurchase.isAcknowledged,
                    originalJson = platformPurchase.originalJson,
                    signature = platformPurchase.signature
                )
                trySend(mapped).isSuccess
            } catch (e: Throwable) {
                // ignore or log
            }
        }

        // on close, clear callback
        awaitClose {
            purchasesUpdatedCallback = null
        }
    }

    actual suspend fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
    }

    // helper: map platform Purchase to your common Purchase (keeps originalJson + signature for server)
    private fun com.android.billingclient.api.Purchase.toCommonPurchase(): Purchase {
        return Purchase(
            productId = this.products.firstOrNull() ?: "",
            purchaseToken = this.purchaseToken,
            orderId = this.orderId ?: "",
            purchaseTime = this.purchaseTime,
            isAcknowledged = this.isAcknowledged,
            originalJson = this.originalJson,
            signature = this.signature
        )
    }
}
