package com.orangebox.kit.admin.role

import com.orangebox.kit.admin.userb.UserB
import com.orangebox.kit.admin.userb.UserBService
import com.orangebox.kit.admin.util.AdminBaseRestService
import com.orangebox.kit.admin.util.SecuredAdmin
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/backofficeRole")
class BackofficeRoleRestService : AdminBaseRestService() {

    @Inject
    private lateinit var userBService: UserBService

    @Inject
    private lateinit var backofficeRoleDAO: BackofficeRoleDAO

    @SecuredAdmin
    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun save(backofficeRole: BackofficeRole): BackofficeRole {
        userBService.saveBackofficeRole(backofficeRole)
        return backofficeRole
    }

    @SecuredAdmin
    @GET
    @Path("/allRoles/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun search(@PathParam("version")version: String): List<BackofficeRole>? {
        val roles = backofficeRoleDAO.search(backofficeRoleDAO.createBuilder().build())
        if (version == "lite") {
            val filtered = roles?.filter { it.name!!.contains("Lite") || it.name!!.contains("ALL") }
            return filtered
        } else {
            val filtered = roles?.filter { !it.name!!.contains("Lite") }
            return filtered
        }
    }

}