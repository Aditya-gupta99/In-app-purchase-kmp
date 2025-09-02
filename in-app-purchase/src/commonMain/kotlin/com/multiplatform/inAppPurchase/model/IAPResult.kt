package com.multiplatform.inAppPurchase.model

sealed class IAPResult<out T> {

    data class Success<T>(val data: T) : IAPResult<T>()

    data class Error(val message: String, val code: Int? = null) : IAPResult<Nothing>()

}