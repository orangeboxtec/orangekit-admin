package com.orangebox.kit.admin.util

import com.orangebox.kit.admin.userb.UserBTokenValidatorService
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.naming.InitialContext

@ApplicationScoped
class TokenValidatorProvider {

    var tokenValidator: TokenValidator? = null

    @Inject
    private lateinit var userBTokenValidatorService: UserBTokenValidatorService

    @ConfigProperty(name = "orangekit.admin.tokenValidator", defaultValue = "ERROR")
    private lateinit var validator: String

    @PostConstruct
    fun init() {
        try {
            tokenValidator = run {
                val ctx = InitialContext()
                val tv = ctx.lookup(validator) as TokenValidator
                tv
            }
        } catch (e: Exception) {
            tokenValidator = userBTokenValidatorService
        }
    }
}