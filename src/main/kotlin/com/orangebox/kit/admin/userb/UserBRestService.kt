package com.orangebox.kit.admin.userb

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.*
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Stateless
@Path("/userBackoffice")
class UserBRestService : AdminBaseRestService() {
    @EJB
    private val userBService: UserBService? = null

    @Context
    private val requestB: HttpServletRequest? = null

    @EJB
    private val configurationService: ConfigurationService? = null

    @EJB
    private val emailService: EmailService? = null

    @POST
    @Path("/authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun authenticate(user: UserB?): LoginInfo {
        return userBService!!.authenticate(user)
    }

    @POST
    @Path("/authenticateMobile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun authenticateMobile(user: UserB?): UserB {
        return userBService!!.authenticateMobile(user)
    }

    @GET
    @Path("/load/{idUserB}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun load(@PathParam("idUserB") idUserB: String?): UserB {
        return userBService!!.retrieve(idUserB)
    }

    @GET
    @SecuredAdmin
    @Path("/menu")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun menu(): List<Role> {
        val userB: UserB = getUserTokenSession()
        return userB.role.getRoles()
    }

    @GET
    @SecuredAdmin
    @Path("/userBSession")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun userBSession(): UserB {
        return getUserTokenSession()
    }

    @GET
    @SecuredAdmin
    @Path("/listUsersByRole/{roleId}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun listUsersByRole(@PathParam("roleId") roleId: String?): List<UserB> {
        return userBService!!.listUsersByRole(roleId)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveUser")
    @Throws(
        Exception::class
    )
    fun saveUser(userB: UserB): UserB {
        userBService!!.saveUser(userB)
        return userB
    }

    @Deprecated("")
    @POST
    @SecuredAdmin
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/changeStatus")
    @Throws(
        Exception::class
    )
    fun changeStatus(userB: UserB) {
        userBService!!.changeStatus(userB.id)
    }

    @GET
    @SecuredAdmin
    @Path("/listAllRoles")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun listAllRoles(): List<BackofficeRole> {
        return userBService!!.listAllRoles()
    }

    @POST
    @Path("/uploadAvatar")
    @Consumes("multipart/form-data")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        IOException::class
    )
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
    @Throws(
        Exception::class
    )
    fun updatePassword(usMon: UserB?) {
        userBService!!.updatePassword(usMon)
    }

    @Deprecated("")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/updatePasswordForgot")
    @Throws(
        Exception::class
    )
    fun updatePasswordForgot(usMon: UserB?) {
        userBService!!.updatePasswordForgot(usMon)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/validateKey")
    @Throws(
        Exception::class
    )
    fun validateKey(userAuthKey: UserAuthKey?): Boolean {
        return userBService!!.validateKey(userAuthKey)
    }

    @GET
    @Path("/showAvatar/{idUser}/{extra}")
    @Produces("image/jpeg")
    @Throws(Exception::class)
    fun showAvatar(@PathParam("idUser") idUser: String?, @PathParam("extra") extra: String?): StreamingOutput {
        return StreamingOutput { out: OutputStream ->
            try {
                var path = userBService!!.pathFiles(idUser) + "/main.jpg"
                if (!File(path).exists()) {
                    path = userBService.pathFiles("") + "default.jpg"
                }
                val `in` = ByteArrayInputStream(FileUtil.readFile(path))
                val buf = ByteArray(16384)
                var len = `in`.read(buf)
                while (len != -1) {
                    out.write(buf, 0, len)
                    len = `in`.read(buf)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @PUT
    @Path("/forgotPassword/{email}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun forgotPassword(@PathParam("email") email: String?) {
        userBService!!.forgotPassword(email)
    }

    @POST
    @SecuredAdmin
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/searchAdmin")
    @Throws(
        Exception::class
    )
    fun searchAdmin(userSearch: UserBSearch): UserBResultSearch {
        if (userSearch.idObj == null) {
            userSearch.idObj = this.getUserTokenSession().getIdObj()
        }
        return userBService!!.searchAdmin(userSearch)
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/search")
    fun search(userBSearch: UserBSearch?): List<UserB> {
        return userBService!!.search(userBSearch)
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/searchResponse")
    fun searchResponse(userBSearch: UserBSearch?): ResponseList<UserB> {
        return userBService!!.searchResponse(userBSearch)
    }
}