package com.appdev.inventoryapp.Utils


enum class AuditActionType {
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    USER_ACTIVATED,
    USER_DEACTIVATED,
    PERMISSIONS_CHANGED,
    ROLE_CHANGED,
    OTHER
}