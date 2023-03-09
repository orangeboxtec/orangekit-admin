package com.orangebox.kit.admin.userb

import com.fasterxml.jackson.databind.ObjectMapper
import com.orangebox.kit.admin.role.BackofficeRole
import com.orangebox.kit.admin.role.Role
import com.orangebox.kit.admin.util.AdminBaseRestService
import com.orangebox.kit.admin.util.SecuredAdmin
import com.orangebox.kit.core.configuration.ConfigurationService
import com.orangebox.kit.core.dto.ResponseList
import com.orangebox.kit.notification.email.EmailService
import java.io.*
import javax.inject.Inject
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("/userBackoffice")
class UserBRestService : AdminBaseRestService() {

    @Inject
    private lateinit var userBService: UserBService

    @Context
    private val requestB: HttpServletRequest? = null

    @Inject
    private lateinit var configurationService: ConfigurationService

    @Inject
    private lateinit var emailService: EmailService

    @POST
    @Path("/authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun authenticate(user: UserB): LoginInfo? {
        return userBService.authenticate(user)
    }

    @POST
    @Path("/authenticateMobile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun authenticateMobile(user: UserB): UserB? {
        return userBService.authenticateMobile(user)
    }

    @GET
    @Path("/load/{idUserB}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun load(@PathParam("idUserB") idUserB: String): UserB? {
        return userBService.retrieve(idUserB)
    }

    @GET
    @SecuredAdmin
    @Path("/menu")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun menu(): List<Role>? {
        val userB: UserB = getUserTokenSession()
        return userB.role?.roles
    }

    @GET
    @SecuredAdmin
    @Path("/userBSession")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun userBSession(): UserB {
        return getUserTokenSession()
    }

    @GET
    @SecuredAdmin
    @Path("/listUsersByRole/{roleId}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun listUsersByRole(@PathParam("roleId") roleId: String): List<UserB?>? {
        return userBService.listUsersByRole(roleId)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveUser")
    fun saveUser(userB: UserB): UserB {
        userBService.saveUser(userB)
        return userB
    }


    @GET
    @SecuredAdmin
    @Path("/listAllRoles")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun listAllRoles(): List<BackofficeRole>? {
        return userBService.listAllRoles()
    }


    @POST
    @Path("/uploadAvatar")
    @Consumes("multipart/form-data")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun uploadAvatar(input: MultipartFormDataInput): String? {
        try {
            val uploadForm: Map<String, List<InputPart>> = input.getFormDataMap()

            //get the object id
            val inputPartsId: InputPart = uploadForm["photo_id"]!![0]
            val photoId: String = inputPartsId.getBody(String::class.java, null)

            //get the config data to crop
            val inputPartsData: InputPart = uploadForm["avatar_data"]!![0]
            val json: String = inputPartsData.getBody(String::class.java, null)
            val mapper = ObjectMapper()
            val photoUpload: PhotoUpload = mapper.readValue(json, PhotoUpload::class.java) as PhotoUpload

            // Get file data to save
            val inputPartsFile: InputPart = uploadForm["avatar_file"]!![0]
            val inputStream: InputStream = inputPartsFile.getBody(InputStream::class.java, null)
            val bytes: ByteArray = IOUtils.toByteArray(inputStream)
            photoUpload.setPhotoBytes(bytes)

            //get the final size
            val finalWidth: Int = configurationService.loadByCode("SIZE_DETAIL_MOBILE").getValueAsInt()
            photoUpload.setFinalWidth(finalWidth)
            val path = userBService!!.pathFiles(photoId)

//            new PhotoUtils().saveImage(photoUpload, path);
            return "{\"state\": 200}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/updatePassword")
    fun updatePassword(usMon: UserB?) {
        userBService.updatePassword(usMon)
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/validateKey")
    fun validateKey(userAuthKey: UserAuthKey?): Boolean {
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
        if (userSearch.idObj == null) {
            userSearch.idObj = this.getUserTokenSession().getIdObj()
        }
        return userBService.searchAdmin(userSearch)
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/search")
    fun search(userBSearch: UserBSearch): List<UserB?>? {
        return userBService.search(userBSearch)
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/searchResponse")
    fun searchResponse(userBSearch: UserBSearch): ResponseList<UserB> {
        return userBService.searchResponse(userBSearch)
    }
}