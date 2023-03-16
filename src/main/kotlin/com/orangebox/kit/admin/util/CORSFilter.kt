package com.orangebox.kit.admin.util

import java.io.IOException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.ext.Provider

@Provider
class CORSFilter : ContainerResponseFilter {
    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        responseContext.headers.putSingle("Access-Control-Allow-Origin", "*")
        responseContext.headers.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
        val reqHeader = requestContext.getHeaderString("Access-Control-Request-Headers")
        if (reqHeader != null && reqHeader !== "") {
            responseContext.headers.putSingle("Access-Control-Allow-Headers", reqHeader)
        }
    }
}