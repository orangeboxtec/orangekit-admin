package com.orangebox.kit.admin.util

interface TokenValidator {
    @Throws(Exception::class)
    fun checkToken(token: String?): Boolean?
}