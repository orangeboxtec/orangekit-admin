package com.orangebox.kit.admin.userb

import java.util.*
import javax.ejb.EJB

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
class UserBTokenValidatorServiceImpl : UserBTokenValidatorService {
    @EJB
    private val userBService: UserBService? = null
    @Throws(Exception::class)
    fun checkToken(token: String): Boolean {
        var validated = true
        val user = userBService!!.retrieveByToken(token)
        if (user!!.token == null || user.token != token || user.tokenExpirationDate!!.before(Date())) {
            validated = false
        }
        return validated
    }
}