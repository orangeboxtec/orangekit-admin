package com.orangebox.kit.admin.test

import com.orangebox.kit.admin.util.TokenValidator
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Alternative


@Alternative
@ApplicationScoped
class TokenValidatorNois: TokenValidator {
    override fun checkToken(token: String): Boolean {
        return true
    }
}