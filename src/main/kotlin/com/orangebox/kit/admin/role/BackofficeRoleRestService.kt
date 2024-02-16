package com.orangebox.kit.admin.role

import com.orangebox.kit.admin.userb.UserBService
import com.orangebox.kit.admin.util.AdminBaseRestService
import com.orangebox.kit.admin.util.SecuredAdmin
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/backofficeRole")
class BackofficeRoleRestService : AdminBaseRestService() {

    @Inject
    private lateinit var userBService: UserBService

    @SecuredAdmin
    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun save(backofficeRole: BackofficeRole): BackofficeRole {
        userBService.saveBackofficeRole(backofficeRole)
        return backofficeRole
    }
}