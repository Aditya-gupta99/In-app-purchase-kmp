package com.multiplatform.inAppPurchase.model

data class SubscriptionOffer(
    val basePlanId: String,
    val offerToken: String,
    val price: String,
    val priceCurrencyCode: String,
    val priceAmountMicros: Long,
    val billingPeriod: String
)