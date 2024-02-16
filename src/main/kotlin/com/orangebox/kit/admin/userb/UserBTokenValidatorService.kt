package com.orangebox.kit.admin.userb

import com.orangebox.kit.admin.util.TokenValidator
import com.orangebox.kit.core.apptoken.AppTokenService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*

@ApplicationScoped
class UserBTokenValidatorService: TokenValidator {

    @Inject
    private lateinit var userBService: UserBService

    @Inject
    private lateinit var appTokenService: AppTokenService

    @ConfigProperty(name = "orangekit.core.applicationtoken", defaultValue = "false")
    private lateinit var appToken: String

    override fun checkToken(token: String): Boolean {
        var validated = true
        println("VALOR DE APP TOKEN: $appToken")
        val user = userBService.retrieveByToken(token)
        if (user?.token == null || user.token != token || user.tokenExpirationDate!!.before(Date())) {
            validated = if(appToken.toBoolean()){
                appTokenService.checkAppToken(token)
            } else {
                false
            }
        }
        return validated
    }
}