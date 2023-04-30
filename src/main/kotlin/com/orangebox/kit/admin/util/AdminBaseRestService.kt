package com.orangebox.kit.admin.util

import com.orangebox.kit.admin.userb.UserB
import com.orangebox.kit.admin.userb.UserBService
import org.jboss.resteasy.reactive.RestHeader
import javax.inject.Inject
import javax.ws.rs.NotAuthorizedException


open class AdminBaseRestService {

    @RestHeader("AUTHORIZATION")
    lateinit var authorizationHeader: String

    @Inject
    private lateinit var userBService: UserBService

    protected val userTokenSession: UserB?
        get() {

            // Check if the HTTP Authorization header is present and formatted correctly 
            if (!authorizationHeader.startsWith("Bearer ")) {
                throw NotAuthorizedException("Authorization header must be provided")
            }

            // Extract the token from the HTTP Authorization header
            val token = authorizationHeader.substring("Bearer".length).trim { it <= ' ' }
            if(userBService.checkToken(token) == true){
                return userBService.retrieveByToken(token)
            }

            throw NotAuthorizedException("invalid_token")
        }
}