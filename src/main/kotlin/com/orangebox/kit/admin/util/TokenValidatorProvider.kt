package com.orangebox.kit.admin.util

import com.orangebox.kit.admin.userb.UserBTokenValidatorService
import java.util.*
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.inject.Singleton
import javax.naming.InitialContext

@Singleton
@ApplicationScoped
class TokenValidatorProvider {

    var tokenValidator: TokenValidator? = null

    @Inject
    private lateinit var userBTokenValidatorService: UserBTokenValidatorService

    @PostConstruct
    fun init() {
        try {
            val bundle = ResourceBundle.getBundle("db")
            val validator = bundle.getString("tokenValidator")
            tokenValidator = if (validator == null) {
                userBTokenValidatorService
            } else {
                val ctx = InitialContext()
                val tv = ctx.lookup(validator) as TokenValidator
                tv
            }
        } catch (e: Exception) {
            tokenValidator = userBTokenValidatorService
            e.printStackTrace()
        }
    }
}