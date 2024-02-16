package com.orangebox.kit.admin.userb

import com.orangebox.kit.admin.role.BackofficeRole
import com.orangebox.kit.admin.role.Role
import com.orangebox.kit.admin.util.AdminBaseRestService
import com.orangebox.kit.admin.util.SecuredAdmin
import com.orangebox.kit.authkey.UserAuthKey
import com.orangebox.kit.core.apptoken.SecuredApp
import com.orangebox.kit.core.dto.ResponseList
import com.orangebox.kit.core.file.FileUpload
import io.quarkus.security.UnauthorizedException
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.http.HttpRequest

@Path("/userBackoffice")
class UserBRestService : AdminBaseRestService() {

    @Inject
    private lateinit var userBService: UserBService

    @Context
    private val requestB: HttpRequest? = null

    @ConfigProperty(name = "orangekit.admin.ssoflow", defaultValue = "false")
    private lateinit var ssoFlow: String

    @POST
    @Path("/authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun authenticate(user: UserB): UserB? {
        return userBService.authenticateMobile(user)
    }

    @SecuredAdmin
    @GET
    @Path("/load/{idUserB}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun load(@PathParam("idUserB") idUserB: String): UserB? {
        if(ssoFlow.toBoolean()){
            throw UnauthorizedException("sso_activated")
        }
        return userBService.retrieve(idUserB)
    }

    @GET
    @SecuredAdmin
    @Path("/menu")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun menu(): List<Role>? {
        val userB = userTokenSession
        return userB?.role?.roles
    }

    @GET
    @SecuredAdmin
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun logout() {
        val userB = userTokenSession
        if (userB != null) {
            userBService.logout(userB)
        }
    }

    @GET
    @SecuredAdmin
    @Path("/userBSession")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun userBSession(): UserB? {
        return userTokenSession
    }

    @GET
    @SecuredAdmin
    @Path("/listUsersByRole/{roleId}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun listUsersByRole(@PathParam("roleId") roleId: String): List<UserB?>? {
        return userBService.listUsersByRole(roleId)
    }

    @SecuredAdmin
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveUser")
    fun saveUser(userB: UserB): UserB {
        userBService.saveUser(userB)
        return userB
    }

    @SecuredAdmin
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveInfo")
    fun saveInfo(userB: UserB): UserB {
        userBService.saveInfo(userB)
        return userB
    }

    @SecuredApp
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveAnonymous")
    fun saveAnonymous(userB: UserB): UserB {
        userBService.saveAnonymous(userB)
        return userB
    }


    @GET
    @SecuredAdmin
    @Path("/listAllRoles")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun listAllRoles(): List<BackofficeRole?>? {
        return userBService.listAllRoles()
    }

    @SecuredAdmin
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/updatePassword")
    fun updatePassword(usMon: UserB) {
        userBService.updatePassword(usMon)
    }

    @SecuredAdmin
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/updatePasswordForgot")
    fun updatePasswordForgot(usMon: UserB) {
        userBService.updatePasswordForgot(usMon)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/validateKey")
    fun validateKey(userAuthKey: UserAuthKey): UserB? {
        return userBService.validateKey(userAuthKey)
    }

    @PUT
    @Path("/forgotPassword/{email}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun forgotPassword(@PathParam("email") email: String) {
        userBService.forgotPassword(email)
    }

    @POST
    @SecuredAdmin
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/searchAdmin")
    fun searchAdmin(userSearch: UserBSearch): UserBResultSearch? {
        if(ssoFlow.toBoolean()){
            throw UnauthorizedException("sso_activated")
        }
        if (userSearch.idObj == null) {
            userSearch.idObj = userTokenSession?.idObj
        }
        return userBService.searchAdmin(userSearch)
    }

    @SecuredAdmin
    @POST
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/search")
    fun search(userBSearch: UserBSearch): List<UserB?>? {
        return userBService.search(userBSearch)
    }

    @SecuredAdmin
    @POST
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/searchResponse")
    fun searchResponse(userBSearch: UserBSearch): ResponseList<UserB>? {
        return userBService.searchResponse(userBSearch)
    }


    @SecuredAdmin
    @POST
    @Consumes("application/json")
    @Path("/saveAvatar")
    fun saveAvatar(fileUpload: FileUpload) {
        return userBService.saveAvatar(fileUpload)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/autoLogin")
    fun autoLogin(usMon: UserB): UserB? {
        return userBService.autoLogin(usMon)
    }
}