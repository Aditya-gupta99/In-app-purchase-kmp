package com.multiplatform.inAppPurchase.model

data class Product(

    val id: String,

    val title: String,

    val description: String,

    val price: String,

    val priceCurrencyCode: String,

    val priceAmountMicros: Long,

    val type: String,

    val productType: ProductType = ProductType.ONE_TIME_PURCHASE,

    val subscriptionOffers: List<SubscriptionOffer> = emptyList()
)