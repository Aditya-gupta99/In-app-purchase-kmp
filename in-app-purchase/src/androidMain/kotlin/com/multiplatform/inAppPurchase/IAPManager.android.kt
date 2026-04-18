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
import com.multiplatform.inAppPurchase.model.ProductType
import com.multiplatform.inAppPurchase.model.Purchase
import com.multiplatform.inAppPurchase.model.SubscriptionOffer
import kotlinx.coroutines.CancellableContinuation
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

    private var purchaseFlowContinuation: CancellableContinuation<IAPResult<Unit>>? = null
    private var currentPurchaseId = 0

    /**
     * Lock object to synchronize access to [purchaseFlowContinuation].
     * This prevents race conditions when multiple BillingClient callbacks
     * fire simultaneously on different threads.
     */
    private val continuationLock = Any()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val continuation: CancellableContinuation<IAPResult<Unit>>

        // Atomically read-and-clear the continuation so only ONE thread can consume it
        synchronized(continuationLock) {
            val c = purchaseFlowContinuation ?: return@PurchasesUpdatedListener
            purchaseFlowContinuation = null
            continuation = c
        }

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { p -> purchasesUpdatedCallback?.invoke(p) }
                continuation.resume(IAPResult.Success(Unit))
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                continuation.resume(
                    IAPResult.Error("User cancelled the purchase", billingResult.responseCode)
                )
            }
            else -> {
                continuation.resume(
                    IAPResult.Error(
                        "Purchase failed: ${billingResult.debugMessage}",
                        billingResult.responseCode
                    )
                )
            }
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

        // ── FIX: disconnect any previously-created BillingClient ──
        // Google Play dispatches PurchasesUpdatedListener to EVERY connected
        // BillingClient in the process. Leaving old clients alive causes the
        // listener to fire N times (once per client), which produces the
        // duplicate "User cancelled the purchase" errors the caller observes.
        billingClient?.endConnection()
        billingClient = null

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
    actual suspend fun getProducts(
        productIds: List<String>,
        productType: ProductType
    ): IAPResult<List<Product>> = suspendCancellableCoroutine { continuation ->

        val client = billingClient ?: run {
            continuation.resume(IAPResult.Error("Billing client not initialized"))
            return@suspendCancellableCoroutine
        }

        val billingProductType = productType.toBillingProductType()

        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(billingProductType)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsResponse ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val mapped = productDetailsResponse.productDetailsList.map { details ->
                    productDetailsCache[details.productId] = details

                    val isSubscription = details.productType == BillingClient.ProductType.SUBS

                    // Map subscription offers
                    val subscriptionOffers = if (isSubscription) {
                        details.subscriptionOfferDetails?.map { offer ->
                            val pricingPhase = offer.pricingPhases.pricingPhaseList.lastOrNull()
                            SubscriptionOffer(
                                basePlanId = offer.basePlanId,
                                offerToken = offer.offerToken,
                                price = pricingPhase?.formattedPrice ?: "",
                                priceCurrencyCode = pricingPhase?.priceCurrencyCode ?: "",
                                priceAmountMicros = pricingPhase?.priceAmountMicros ?: 0L,
                                billingPeriod = pricingPhase?.billingPeriod ?: ""
                            )
                        } ?: emptyList()
                    } else emptyList()

                    // For one-time products use oneTimePurchaseOfferDetails, for subs use first offer
                    val displayPrice = if (isSubscription) {
                        details.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.lastOrNull()
                            ?.formattedPrice ?: ""
                    } else {
                        details.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                    }

                    val displayCurrency = if (isSubscription) {
                        details.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.lastOrNull()
                            ?.priceCurrencyCode ?: ""
                    } else {
                        details.oneTimePurchaseOfferDetails?.priceCurrencyCode ?: ""
                    }

                    val displayMicros = if (isSubscription) {
                        details.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.lastOrNull()
                            ?.priceAmountMicros ?: 0L
                    } else {
                        details.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L
                    }

                    Product(
                        id = details.productId,
                        title = details.title,
                        description = details.description,
                        price = displayPrice,
                        priceCurrencyCode = displayCurrency,
                        priceAmountMicros = displayMicros,
                        type = details.productType,
                        productType = productType,
                        subscriptionOffers = subscriptionOffers
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
        product: Product,
        basePlanId: String?,
        userId: String?
    ): IAPResult<Unit> = suspendCancellableCoroutine { continuation ->

        val client = billingClient ?: run {
            continuation.resume(IAPResult.Error("Billing client not initialized"))
            return@suspendCancellableCoroutine
        }

        val act = activity ?: run {
            continuation.resume(IAPResult.Error("Activity not set"))
            return@suspendCancellableCoroutine
        }

        val productDetails = productDetailsCache[product.id] ?: run {
            continuation.resume(IAPResult.Error("ProductDetails not found for ${product.id}"))
            return@suspendCancellableCoroutine
        }

        val thisPurchaseId = ++currentPurchaseId

        // Store the continuation so PurchasesUpdatedListener can resume it
        synchronized(continuationLock) {
            purchaseFlowContinuation = continuation
        }

        // If the coroutine is cancelled externally, clear the stored continuation
        continuation.invokeOnCancellation {
            synchronized(continuationLock) {
                if (currentPurchaseId == thisPurchaseId) {
                    purchaseFlowContinuation = null
                }
            }
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        when (product.productType) {
            ProductType.SUBSCRIPTION -> {
                val selectedOffer = if (basePlanId != null) {
                    productDetails.subscriptionOfferDetails?.find { it.basePlanId == basePlanId }
                } else {
                    productDetails.subscriptionOfferDetails?.firstOrNull()
                }

                if (selectedOffer == null) {
                    synchronized(continuationLock) { purchaseFlowContinuation = null }
                    continuation.resume(IAPResult.Error("No subscription offer found for basePlanId: $basePlanId"))
                    return@suspendCancellableCoroutine
                }

                productParamsBuilder.setOfferToken(selectedOffer.offerToken)
            }

            ProductType.ONE_TIME_PURCHASE -> {
                productDetails.oneTimePurchaseOfferDetails?.offerToken?.let {
                    if (it.isNotEmpty()) productParamsBuilder.setOfferToken(it)
                }
            }
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .setObfuscatedAccountId(userId ?: "")
            .build()

        // This only launches the billing sheet — does NOT mean purchase succeeded
        val result = client.launchBillingFlow(act, billingFlowParams)

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            // Sheet failed to open — resume immediately with error
            synchronized(continuationLock) { purchaseFlowContinuation = null }
            continuation.resume(
                IAPResult.Error(
                    "Failed to launch billing flow: ${result.debugMessage}",
                    result.responseCode
                )
            )
        }

        // If sheet opened successfully (OK), we do NOT resume here.
        // The coroutine stays suspended until PurchasesUpdatedListener fires.
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
    actual suspend fun getPurchases(
        productType: ProductType
    ): IAPResult<List<Purchase>> = suspendCancellableCoroutine { continuation ->
        val client = billingClient ?: run {
            continuation.resume(IAPResult.Error("Billing client not initialized"))
            return@suspendCancellableCoroutine
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType.toBillingProductType())
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
                        signature = p.signature,
                        productType = productType,
                        isAutoRenewing = p.isAutoRenewing
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

private fun ProductType.toBillingProductType(): String = when (this) {
    ProductType.ONE_TIME_PURCHASE -> BillingClient.ProductType.INAPP
    ProductType.SUBSCRIPTION -> BillingClient.ProductType.SUBS
}
