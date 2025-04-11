package com.appdev.inventoryapp.Utils

sealed class ResultState<out T> {
    data class Success<out R>(val data:R) : ResultState<R>()
    data class Failure(val message:Throwable) : ResultState<Nothing>()
    data object Loading : ResultState<Nothing>()
}