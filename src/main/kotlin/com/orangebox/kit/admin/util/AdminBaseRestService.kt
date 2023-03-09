package com.orangebox.kit.admin.util

import com.orangebox.kit.admin.userb.UserB
import com.orangebox.kit.admin.userb.UserBService
import javax.inject.Inject
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders

open class AdminBaseRestService {

    @Context
    private val request: HttpServletRequest? = null

    @Inject
    private lateinit var userBService: UserBService

    @get:Throws(Exception::class)
    protected val userTokenSession: UserB?
        protected get() {

            // Get the HTTP Authorization header from the request
            val authorizationHeader: String = request.getHeader(HttpHeaders.AUTHORIZATION)

            // Check if the HTTP Authorization header is present and formatted correctly 
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw NotAuthorizedException("Authorization header must be provided")
            }

            // Extract the token from the HTTP Authorization header
            val token = authorizationHeader.substring("Bearer".length).trim { it <= ' ' }
            return userBService.retrieveByToken(token)
        }
}