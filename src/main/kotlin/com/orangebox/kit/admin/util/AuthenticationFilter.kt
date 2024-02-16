package com.orangebox.kit.admin.util

import com.orangebox.kit.admin.userb.UserBService
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider

@SecuredAdmin
@Provider
@Priority(Priorities.AUTHENTICATION)
class AuthenticationFilter : ContainerRequestFilter {


    @Inject
    private lateinit var userBService: UserBService

    override fun filter(requestContext: ContainerRequestContext) {

        // Get the HTTP Authorization header from the request
        val authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)

        // Check if the HTTP Authorization header is present and formatted correctly 
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw NotAuthorizedException("Authorization header must be provided")
        }

        // Extract the token from the HTTP Authorization header
        val token = authorizationHeader.substring("Bearer".length).trim { it <= ' ' }
        try {
            // Validate the token
            validateToken(token)
        } catch (e: Exception) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
        }
    }

    private fun validateToken(token: String) {
        val validated: Boolean = userBService.checkToken(token) == true
        if (!validated) {
            throw Exception("invalid_token")
        }
    }

    private fun validateUrl(token: String, url: String) {
        val validated: Boolean = userBService.checkAcessRole(token, url) == true
        if (!validated) {
            throw Exception("unauthorized_url")
        }
    }
}