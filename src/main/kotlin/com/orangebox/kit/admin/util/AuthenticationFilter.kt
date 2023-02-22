package com.orangebox.kit.admin.util

import org.startupkit.admin.userb.UserBService
import java.io.IOException
import javax.annotation.Priority
import javax.ws.rs.Priorities
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@SecuredAdmin
@Provider
@Priority(Priorities.AUTHENTICATION)
class AuthenticationFilter : ContainerRequestFilter {
    @Context
    private val request: HttpServletRequest? = null

    @EJB
    private val userBFacade: UserBService? = null
    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext) {

        // Get the HTTP Authorization header from the request
        val authorizationHeader: String = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)

        // Check if the HTTP Authorization header is present and formatted correctly 
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw NotAuthorizedException("Authorization header must be provided")
        }

        // Extract the token from the HTTP Authorization header
        val token = authorizationHeader.substring("Bearer".length).trim { it <= ' ' }
        try {

            // Validate the token
            validateToken(token)
            request.getHeader(HttpHeaders.AUTHORIZATION)

            // Validate the url
//            validateUrl(token, requestContext.getUriInfo().getPath());
        } catch (e: Exception) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
        }
    }

    @Throws(Exception::class)
    private fun validateToken(token: String) {
        val validated: Boolean = userBFacade.checkToken(token)
        if (!validated) {
            throw Exception("Invalid Token")
        }
    }

    @Throws(Exception::class)
    private fun validateUrl(token: String, url: String) {
        val validated: Boolean = userBFacade.checkAcessRole(token, url)
        if (!validated) {
            throw Exception("Unauthorized Url")
        }
    }
}