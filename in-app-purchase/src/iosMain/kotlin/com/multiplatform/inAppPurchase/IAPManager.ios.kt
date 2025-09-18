package com.multiplatform.inAppPurchase

import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import com.multiplatform.inAppPurchase.model.Purchase
import kotlinx.coroutines.flow.Flow

actual class IAPManager actual constructor() {
    actual suspend fun initialize(): IAPResult<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun getProducts(productIds: List<String>): IAPResult<List<Product>> {
        TODO("Not yet implemented")
    }

    actual suspend fun launchPurchaseFlow(product: Product): IAPResult<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun consumePurchase(purchase: Purchase): IAPResult<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun acknowledgePurchase(purchase: Purchase): IAPResult<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun getPurchases(): IAPResult<List<Purchase>> {
        TODO("Not yet implemented")
    }

    actual suspend fun restorePurchases(): IAPResult<List<Purchase>> {
        TODO("Not yet implemented")
    }

    actual fun getPurchaseUpdates(): Flow<Purchase> {
        TODO("Not yet implemented")
    }

    actual suspend fun disconnect() {
    }
}