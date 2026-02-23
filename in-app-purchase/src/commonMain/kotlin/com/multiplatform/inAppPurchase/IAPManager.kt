package com.multiplatform.inAppPurchase

import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import com.multiplatform.inAppPurchase.model.ProductType
import com.multiplatform.inAppPurchase.model.Purchase
import kotlinx.coroutines.flow.Flow

expect class IAPManager() {

    suspend fun initialize(): IAPResult<Unit>

    suspend fun getProducts(
        productIds: List<String>,
        productType: ProductType = ProductType.ONE_TIME_PURCHASE
    ): IAPResult<List<Product>>

    suspend fun launchPurchaseFlow(
        product: Product,
        basePlanId: String? = null
    ): IAPResult<Unit>

    suspend fun consumePurchase(
        purchase: Purchase
    ): IAPResult<Unit>

    suspend fun acknowledgePurchase(
        purchase: Purchase
    ): IAPResult<Unit>

    suspend fun getPurchases(
        productType: ProductType = ProductType.ONE_TIME_PURCHASE
    ): IAPResult<List<Purchase>>

    suspend fun restorePurchases(): IAPResult<List<Purchase>>

    fun getPurchaseUpdates(): Flow<Purchase>

    suspend fun disconnect()
}