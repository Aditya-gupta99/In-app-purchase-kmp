package com.multiplatform.inAppPurchase.model

data class Product(

    val id: String,

    val title: String,

    val description: String,

    val price: String,

    val priceCurrencyCode: String,

    val priceAmountMicros: Long,

    val type: String
)