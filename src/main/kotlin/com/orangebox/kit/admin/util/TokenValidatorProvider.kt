package com.orangebox.kit.admin.util

import org.startupkit.admin.userb.UserBTokenValidatorService
import java.util.*
import javax.annotation.PostConstruct
import javax.naming.InitialContext

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
class TokenValidatorProvider {
    @get:Lock(LockType.READ)
    var tokenValidator: TokenValidator? = null
        private set

    @EJB
    private val userBTokenValidatorService: UserBTokenValidatorService? = null
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