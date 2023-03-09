package com.orangebox.kit.admin.util

interface TokenValidator {

    fun checkToken(token: String): Boolean
}