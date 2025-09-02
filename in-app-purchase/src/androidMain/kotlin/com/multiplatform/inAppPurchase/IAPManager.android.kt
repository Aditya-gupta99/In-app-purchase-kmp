package com.multiplatform.inAppPurchase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
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

    fun setContext(context: Context, activity: Activity? = null) {
        this.context = context
        this.activity = activity
    }

    actual suspend fun initialize(): IAPResult<Unit> = suspendCancellableCoroutine { continuation ->

        val context = this.context ?: run {
            continuation.resume(IAPResult.Error("Context not set"))
            return@suspendCancellableCoroutine
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                // Handle purchase updates
            }
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

    actual suspend fun getProducts(productIds: List<String>): IAPResult<List<Product>> =
        suspendCancellableCoroutine { continuation ->

            val client = billingClient ?: run {
                continuation.resume(IAPResult.Error("Billing client not initialized"))
                return@suspendCancellableCoroutine
            }

            val productList = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                    // TODO : Implement this in future

//                continuation.resume(IAPResult.Success(Unit))
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

    actual suspend fun purchaseProduct(purchase: Purchase): IAPResult<Purchase> =
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
                    continuation.resume(IAPResult.Success(purchase))
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

    actual suspend fun getPurchases(): IAPResult<List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            val client = billingClient ?: run {
                continuation.resume(IAPResult.Error("Billing client not initialized"))
                return@suspendCancellableCoroutine
            }

            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            client.queryPurchasesAsync(params) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val purchases = purchasesList.map { purchase ->
                        Purchase(
                            productId = purchase.products.firstOrNull() ?: "",
                            purchaseToken = purchase.purchaseToken,
                            orderId = purchase.orderId ?: "",
                            purchaseTime = purchase.purchaseTime,
                            isAcknowledged = purchase.isAcknowledged
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

    actual suspend fun restorePurchases(): IAPResult<List<Purchase>> {
        // On Android, this is the same as getPurchases()
        return getPurchases()
    }

    actual fun getPurchaseUpdates(): Flow<Purchase> = callbackFlow {
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { purchase ->
                    val mappedPurchase = Purchase(
                        productId = purchase.products.firstOrNull() ?: "",
                        purchaseToken = purchase.purchaseToken,
                        orderId = purchase.orderId ?: "",
                        purchaseTime = purchase.purchaseTime,
                        isAcknowledged = purchase.isAcknowledged
                    )
                    trySend(mappedPurchase)
                }
            }
        }
        awaitClose { }
    }

    actual suspend fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
    }

    private fun com.android.billingclient.api.Purchase.toCommonPurchase(): Purchase {
        return Purchase(
            productId = this.products.firstOrNull() ?: "",
            purchaseToken = this.purchaseToken,
            orderId = this.orderId ?: "",
            purchaseTime = this.purchaseTime,
            isAcknowledged = this.isAcknowledged
        )
    }

}