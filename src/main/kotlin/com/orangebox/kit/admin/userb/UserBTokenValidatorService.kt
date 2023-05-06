package com.orangebox.kit.admin.userb

import com.orangebox.kit.admin.util.TokenValidator
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject


@ApplicationScoped
class UserBTokenValidatorService: TokenValidator {

    @Inject
    private lateinit var userBService: UserBService

    override fun checkToken(token: String): Boolean {
        var validated = true
        val user = userBService.retrieveByToken(token)
        if (user!!.token == null || user.token != token || user.tokenExpirationDate!!.before(Date())) {
            validated = false
        }
        return validated
    }
}