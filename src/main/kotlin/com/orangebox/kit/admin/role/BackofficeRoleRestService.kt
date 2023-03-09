package com.orangebox.kit.admin.role

import com.orangebox.kit.admin.util.AdminBaseRestService
import javax.inject.Inject
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/backofficeRole")
class BackofficeRoleRestService : AdminBaseRestService() {

    @Inject
    private lateinit var userBService: UserBService

    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun save(backofficeRole: BackofficeRole): BackofficeRole {
        userBService.saveBackofficeRole(backofficeRole)
        return backofficeRole
    }
}