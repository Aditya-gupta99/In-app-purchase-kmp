package com.multiplatform.inAppPurchase.model

data class Purchase(

    val productId: String,

    val purchaseToken: String,

    val orderId: String,

    val purchaseTime: Long,

    val isAcknowledged: Boolean
)