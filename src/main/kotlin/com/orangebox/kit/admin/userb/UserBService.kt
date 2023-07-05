package com.orangebox.kit.admin.userb

import com.jayway.jsonpath.JsonPath
import com.orangebox.kit.admin.role.BackofficeRole
import com.orangebox.kit.admin.role.BackofficeRoleDAO
import com.orangebox.kit.admin.util.TokenValidatorProvider
import com.orangebox.kit.authkey.UserAuthKey
import com.orangebox.kit.authkey.UserAuthKeyService
import com.orangebox.kit.authkey.UserAuthKeyTypeEnum
import com.orangebox.kit.core.bucket.BucketService
import com.orangebox.kit.core.configuration.ConfigurationService
import com.orangebox.kit.core.dao.OperationEnum
import com.orangebox.kit.core.dao.SearchBuilder
import com.orangebox.kit.core.dto.ResponseList
import com.orangebox.kit.core.exception.BusinessException
import com.orangebox.kit.core.file.FileUpload
import com.orangebox.kit.core.user.UserCard
import com.orangebox.kit.core.utils.SecUtils
import com.orangebox.kit.notification.NotificationBuilder
import com.orangebox.kit.notification.NotificationService
import com.orangebox.kit.notification.TypeSendingNotificationEnum
import com.orangebox.kit.notification.email.data.EmailDataTemplate
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import kotlin.collections.HashMap


@ApplicationScoped
class UserBService {

    @Inject
    private lateinit var configurationService: ConfigurationService

    @Inject
    private lateinit var userAuthKeyService: UserAuthKeyService

    @Inject
    private lateinit var notificationService: NotificationService

    @Inject
    private lateinit var bucketService: BucketService

    @Inject
    private lateinit var userBDAO: UserBDAO

    @Inject
    private lateinit var backofficeRoleDAO: BackofficeRoleDAO

    @Inject
    protected lateinit var tokenValidatorProvider: TokenValidatorProvider

    @ConfigProperty(name = "orangekit.admin.email.welcome.templateid", defaultValue = "ERROR")
    private lateinit var welcomeEmailTemplateId: String

    @ConfigProperty(name = "orangekit.admin.email.forgotpassword.templateid", defaultValue = "ERROR")
    private lateinit var forgotEmailTemplateId: String

    @ConfigProperty(name = "orangekit.admin.email.updatedpassword.templateid", defaultValue = "ERROR")
    private lateinit var updatedPasswordTemplateId: String

    @ConfigProperty(name = "orangekit.core.projecturl", defaultValue = "http://localhost:4200")
    private lateinit var projectUrl: String

    @ConfigProperty(name = "orangekit.admin.ssoflow", defaultValue = "false")
    private lateinit var ssoFlow: String

    @ConfigProperty(name = "orangekit.admin.recaptcha.validation", defaultValue = "false")
    private lateinit var recaptchaValidation: String

    @ConfigProperty(name = "orangekit.admin.recaptcha.secret", defaultValue = "false")
    private lateinit var recaptchaSecret: String

    @ConfigProperty(name = "orangekit.admin.user.anonymous", defaultValue = "false")
    private lateinit var userAnonymous: String




    private val QUANTITY_PAGE = 12
    @PostConstruct
    fun pos() {
        Logger.getLogger("org.mongodb.driver").level = Level.SEVERE
        Logger.getLogger("org.hibernate.search.reader.impl.ManagedMultiReader").level = Level.SEVERE
    }

    fun authenticateMobile(user: UserB): UserB {
        return authenticateMobile(user, user.password!!)
    }

    fun authenticateMobile(user: UserB, password: String): UserB {

        if(ssoFlow.toBoolean()){
            throw BusinessException("sso_flow_enabled")
        }

        validateRecaptcha(user)

        var userDB =  userBDAO.retrieve(userBDAO.createBuilder()
            .appendParamQuery("email", user.email!!)
            .appendParamQuery("status", "ACTIVE")
            .build())

        if(userDB == null){
            userDB =  userBDAO.retrieve(userBDAO.createBuilder()
                .appendParamQuery("username", user.email!!)
                .appendParamQuery("status", "ACTIVE")
                .build()) ?: throw BusinessException("user_not_found")
        }

        val passHash: String = SecUtils.generateHash(userDB.salt, password)
        if (userDB.password != passHash) {
            throw BusinessException("invalid_password")
        }
        generateSession(userDB)
        return if (user.status != UserBStatusEnum.BLOCKED) {
            userDB
        } else {
            throw BusinessException("user_blocked")
        }
    }

    fun validateRecaptcha(user: UserB){
        if(recaptchaValidation.toBoolean()){
            if(user.respRecaptcha == null){
                throw BusinessException("field_respRecaptcha_required")
            }

            val url = URL("https://www.google.com/recaptcha/api/siteverify")
            val con: HttpURLConnection = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            con.doOutput = true

            val body = "secret=$recaptchaSecret&response=${user.respRecaptcha}"
            val wr = OutputStreamWriter(con.outputStream)
            wr.write(body)
            wr.flush()
            val resp = JsonPath.read(String(con.inputStream.readAllBytes()), "success") as Boolean
            if(!resp){
                throw BusinessException("invalid_recaptcha_key")
            }
        }
    }

    fun generateSession(user: UserB){
        user.token = UUID.randomUUID().toString()
        val expCal = Calendar.getInstance()
        expCal.add(Calendar.HOUR, 12)
        user.tokenExpirationDate = expCal.time
        userBDAO.update(user)
        if (user.idRole != null) {
            val role = backofficeRoleDAO.retrieve(BackofficeRole(user.idRole))
            user.role = role
        }
    }

    fun logout(user: UserB){
        user.token = null
        user.tokenExpirationDate = null
        userBDAO.update(user)
    }

    fun authenticate(user: UserB): LoginInfo? {
        var info: LoginInfo?
        val userDB = authenticateMobile(user)
        info = LoginInfo()
        info.token = userDB.token
        info.userID = userDB.id
        info.idObj = userDB.idObj
        if (userDB.role != null && userDB.role?.roles != null) {
            val role = userDB.role?.roles?.stream()
                ?.filter { p -> p.fgHome != null && p.fgHome!! }
                ?.findFirst()
                ?.orElse(null)
            if (role != null) {
                info.urlHome = role.url
            }
        }
        return if (userDB.status != UserBStatusEnum.BLOCKED) {
            info
        } else {
            throw BusinessException("user_blocked")
        }
    }

    fun createToken(userDB: UserB) {
        userDB.token = UUID.randomUUID().toString()
        val expCal = Calendar.getInstance()
        expCal.add(Calendar.HOUR, 12)
        userDB.tokenExpirationDate = expCal.time
        userBDAO.update(userDB)
    }

    fun checkToken(token: String): Boolean? {
        return tokenValidatorProvider.tokenValidator?.checkToken(token)
    }

    fun checkAcessRole(token: String, url: String): Boolean? {
        var validated = false
        val user = retrieveByToken(token)
        if (user!!.role != null && user.role?.roles != null) {
            for (role in user.role?.roles!!) {
                if (url.contains(role.url!!)) {
                    validated = true
                    break
                }
            }
        }
        return validated
    }

    fun createNewUser(user: UserB): UserB? {
        return createNewUser(user, null)
    }

    fun createNewUser(user: UserB, templateId: Int?): UserB? {
        val templateEmail = templateId ?: welcomeEmailTemplateId.toInt()
        var userDB: UserB? = null
        if (user.email != null && user.email != "") {
            userDB = retrieveByEmail(user.email!!)
        }
        if (userDB != null) {
            throw BusinessException("email_already_registred")
        }
        if (user.username != null && user.username != "") {
            userDB = userBDAO.retrieve(userBDAO.createBuilder()
                .appendParamQuery("username", user.username!!)
                .build())
        }
        if (userDB != null) {
            throw BusinessException("username_already_registred")
        }
        if (user.status == null) {
            user.status = UserBStatusEnum.ACTIVE
        }
        if (user.language == null) {
            user.language = "pt"
        }
        if (user.password != null) {
            user.salt = SecUtils.salt
            user.password = SecUtils.generateHash(user.salt, user.password!!)
        }
        user.creationDate = Date()
        if (user.role != null) {
            user.role = retrieveRole(user.role!!.id)
        }
        userBDAO.insert(user)
        val key = userAuthKeyService.createKey(user.id!!, UserAuthKeyTypeEnum.EMAIL)
        if (user.language == null) {
            user.language = "pt_BR"
        }

        if (user.password == null) {
            val language = user.language!!.substring(1)
            val link = "$projectUrl/pages/email_forgot_password_userb?l=$language&k=${key.key!!}&u=${user.id!!}&t=${key.type}"

            if(welcomeEmailTemplateId == "ERROR"){
                throw IllegalArgumentException("orangekit.admin.email.welcome.templateid must be provided in .env")
            }

            notificationService.sendNotification(
                NotificationBuilder()
                    .setTo(user)
                    .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                    .setFgAlertOnly(true)
                    .setEmailDataTemplate(object : EmailDataTemplate {
                        override val data: Map<String?, Any?>
                            get() {
                                val params: MutableMap<String?, Any?> = HashMap()
                                params["user_name"] = user.name
                                params["confirmation_link"] = link
                                return params
                            }
                        override val templateId: Int
                            get() = templateEmail
                    })
                    .build()
            )
        }
        return user
    }

    fun updateUser(user: UserB) {
        var userDBEmail: UserB? = null
        if (user.email != null && user.email != "") {
            userDBEmail = retrieveByEmail(user.email!!)
        }
        if (userDBEmail != null && userDBEmail.id != user.id) {
            throw BusinessException("email_already_registred")
        }
        val userDB = userBDAO.retrieve(UserB(user.id))
            ?: if(ssoFlow.toBoolean()){
                createNewUser(user)
                return
            }
            else {
                throw BusinessException("email_not_found")
            }

        if (user.emailConfirmed != null) {
            userDB.emailConfirmed = user.emailConfirmed
        }
        if (user.phoneConfirmed != null) {
            userDB.phoneConfirmed = user.phoneConfirmed
        }
        if (user.userConfirmed != null) {
            userDB.userConfirmed = user.userConfirmed
        }
        if (user.creationDate != null) {
            userDB.creationDate = user.creationDate
        }
        if (user.email != null) {
            userDB.email = user.email
        }
        if (user.info != null) {
            userDB.info = user.info
        }
        if (user.name != null) {
            userDB.name = user.name
        }
        if (user.lastName != null) {
            userDB.lastName = user.lastName
        }
        if (user.document != null) {
            userDB.document = user.document
        }
        if (user.oldPassword != null) {
            userDB.oldPassword = user.oldPassword
        }
        if (user.phoneCountryCode != null) {
            userDB.phoneCountryCode = user.phoneCountryCode
        }
        if (user.phoneNumber != null) {
            userDB.phoneNumber = user.phoneNumber
        }
        if (user.idRole != null) {
            userDB.idRole = user.idRole
        }
        if (user.role != null) {
            val role = backofficeRoleDAO.retrieve(user.role!!)
            userDB.role = role
        }
        if (user.status != null) {
            userDB.status = user.status
        }
        if (user.token != null) {
            userDB.token = user.token
        }
        if (user.tokenExpirationDate != null) {
            userDB.tokenExpirationDate = user.tokenExpirationDate
        }
        if (user.idObj != null) {
            userDB.idObj = user.idObj
        }
        if (user.urlImage != null) {
            userDB.urlImage = user.urlImage
        }
        userBDAO.update(userDB)
    }

    fun updatePassword(user: UserB) {
        val userBase = authenticateMobile(user, user.oldPassword!!)
        userBase.salt = SecUtils.salt
        userBase.password = SecUtils.generateHash(userBase.salt, user.password!!)
        userBDAO.update(userBase)
    }

    fun updatePasswordForgot(user: UserB) {
        val userBase = userBDAO.retrieve(user)
        if (userBase != null) {
            val nullPasword = userBase.password == null
            userBase.salt = SecUtils.salt
            userBase.password = SecUtils.generateHash(userBase.salt, user.password!!)
            userBDAO.update(userBase)

            if(nullPasword && updatedPasswordTemplateId != "ERROR"){
                notificationService.sendNotification(
                    NotificationBuilder()
                        .setTo(userBase)
                        .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                        .setFgAlertOnly(true)
                        .setEmailDataTemplate(object : EmailDataTemplate {
                            override val data: Map<String?, Any?>
                                get() {
                                    val params: MutableMap<String?, Any?> = HashMap()
                                    params["user_name"] = userBase.name
                                    return params
                                }
                            override val templateId: Int
                                get() = updatedPasswordTemplateId.toInt()
                        })
                        .build()
                )
            }
        }
    }

    fun saveUser(user: UserB?) {
        if (user!!.id == null) {
            createNewUser(user)
        } else {
            updateUser(user)
        }
    }


    fun saveInfo(user: UserB) {
        val userDB = userBDAO.retrieve(user.id!!) ?: throw BusinessException("user_not_found")
        if(userDB.info == null){
            userDB.info = HashMap()
        }
        user.info?.keys?.forEach { key ->
            userDB.info!![key] = user.info!![key]!!
        }
        userBDAO.update(userDB)
    }

    fun retrieve(id: String?): UserB? {
        return userBDAO.retrieve(UserB(id))
    }

    fun retrieveByEmail(email: String): UserB? {
        return userBDAO.retrieve(userBDAO.createBuilder()
            .appendParamQuery("email", email)
            .build())
    }

    fun retrieveByPhone(phoneNumber: Long): UserB? {
        return userBDAO.retrieve(
            userBDAO.createBuilder()
            .appendParamQuery("phoneNumber", phoneNumber)
            .build()
        )
    }

    fun retrieveByToken(token: String): UserB? {
        val user = userBDAO.retrieve(userBDAO.createBuilder()
            .appendParamQuery("token", token)
            .appendParamQuery("status", UserBStatusEnum.ACTIVE)
            .build())

        if (user?.idRole != null) {
            val role = backofficeRoleDAO.retrieve(BackofficeRole(user.idRole))
            user.role = role
        }
        return user
    }

    fun retrieveRole(id: String?): BackofficeRole? {
        return backofficeRoleDAO.retrieve(BackofficeRole(id))
    }

    fun userCard(idUser: String): UserCard? {
        val user = retrieve(idUser) ?: throw BusinessException("user_not_found")
        return userCard(user)
    }

    fun userCard(user: UserB): UserCard {
        return user.toCard()
    }

    fun listAdminUsers(): List<UserB?>? {
        return userBDAO.search(userBDAO.createBuilder()
            .appendParamQuery("role.fgAdmin", true)
            .build())
    }

    fun listUsersByRole(roleId: String): List<UserB?>? {
        return userBDAO.search(userBDAO.createBuilder()
            .appendParamQuery("role.id", roleId)
            .build())
    }

    fun listAdminRoles(): List<BackofficeRole?>? {
        return backofficeRoleDAO.search(backofficeRoleDAO.createBuilder()
            .appendParamQuery("fgAdmin", true)
            .build())
    }

    fun listOperationalRoles(): List<BackofficeRole?>? {
        return backofficeRoleDAO.search(backofficeRoleDAO.createBuilder()
            .appendParamQuery("fgAdmin", false)
            .build())
    }

    fun listAllRoles(): List<BackofficeRole?>? {
        return backofficeRoleDAO.listAll()
    }

    fun forgotPassword(email: String) {

        if(ssoFlow.toBoolean()){
            throw BusinessException("sso_flow_enabled")
        }

        val user = retrieveByEmail(email) ?: throw BusinessException("user_not_found")
        val key: UserAuthKey = userAuthKeyService.createKey(user.id!!, UserAuthKeyTypeEnum.EMAIL)
        if (user.language == null) {
            user.language = "pt"
            updateUser(user)
        }
        val link = "$projectUrl/pages/email_forgot_password_userb?l=${user.language}&k=${key.key}&u=${user.id}&t=${key.type}"
        if(forgotEmailTemplateId == "ERROR"){
            throw IllegalArgumentException("orangekit.admin.email.forgotpassword.templateid must be provided in .env")
        }
        notificationService.sendNotification(
            NotificationBuilder()
                .setTo(user)
                .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                .setFgAlertOnly(true)
                .setEmailDataTemplate(object : EmailDataTemplate {
                    override val data: Map<String?, Any?>
                        get() {
                            val params: MutableMap<String?, Any?> = HashMap()
                            params["user_name"] = user.name
                            params["confirmation_link"] = link
                            return params
                        }
                    override val templateId: Int
                        get() = forgotEmailTemplateId.toInt()
                })
                .build()


        )
    }

    fun validateKey(key: UserAuthKey): UserB? {
        val validate: Boolean = userAuthKeyService.validateKey(key)
        if (validate) {
            val user = userBDAO.retrieve(UserB(key.idUser))!!
            user.userConfirmed = true
            if (key.type!! == UserAuthKeyTypeEnum.EMAIL) {
                user.emailConfirmed = true
            } else {
                user.phoneConfirmed = true
            }

            generateSession(user)

            userBDAO.update(user)

            return user
        }

        throw BusinessException("invalid_key")
    }

    fun retrieveByOfficeRole(idOffice: String, idBackofficeRole: String): UserB? {
        var user: UserB? = null
        val listUserB = userBDAO.listByOfficeRole(idOffice, idBackofficeRole)
        if (!listUserB.isNullOrEmpty()) {
            user = listUserB[0]
        }
        return user
    }

    fun listByOfficeRole(idOffice: String, idBackofficeRole: String): List<UserB?>? {
        return userBDAO.listByOfficeRole(idOffice, idBackofficeRole)
    }

    fun listByOfficeRoleDepartment(
        idOffice: String,
        idBackofficeRole: String,
        idDepartment: String
    ): List<UserB?>? {
        return userBDAO.listByOfficeRoleDepartment(idOffice, idBackofficeRole, idDepartment)
    }

    fun retrieveByOfficeRoleDepartment(
        idOffice: String,
        idBackofficeRole: String,
        idDepartment: String
    ): UserB? {
        var user: UserB? = null
        val listUserB = userBDAO.listByOfficeRoleDepartment(idOffice, idBackofficeRole, idDepartment)
        if (!listUserB.isNullOrEmpty()) {
            user = listUserB[0]
        }
        return user
    }

    fun sendNotification(userB: UserB?, message: String?) {
        notificationService.sendNotification(
            NotificationBuilder()
                .setTo(userB)
                .setTypeSending(TypeSendingNotificationEnum.APP)
                .setMessage(message)
                .build()
        )
    }

    fun searchAdmin(userSearch: UserBSearch?): UserBResultSearch? {
        return if (userSearch!!.page == null) {
            throw BusinessException("missing_page")
        } else {
            val sb: SearchBuilder = userBDAO.createBuilder()
            if (userSearch.status != null) {
                sb.appendParamQuery("status", userSearch.status!!)
            }
            if (userSearch.idObj != null) {
                sb.appendParamQuery("idObj", userSearch.idObj!!)
            }
            if (userSearch.queryString != null && userSearch.queryString!!.isNotEmpty()) {
                sb.appendParamQuery("name|nameObj|lastName|document", userSearch.queryString!!, OperationEnum.OR_FIELDS_LIKE)
            }
            if (userSearch.type != null) {
                sb.appendParamQuery("type", userSearch.type!!)
            }
            if (userSearch.pageItensNumber != null && userSearch.pageItensNumber!! > 0) {
                sb.setFirst(userSearch.pageItensNumber!! * (userSearch.page!! - 1))
                sb.setMaxResults(userSearch.pageItensNumber)
            } else {
                sb.setFirst(10 * (userSearch.page!! - 1))
                sb.setMaxResults(10)
            }
            sb.appendSort("name", 1)
            val list = userBDAO.search(sb.build())
            val totalAmount = totalAmount(sb)
            val pageQuantity: Long = if (userSearch.pageItensNumber != null && userSearch.pageItensNumber!! > 0) {
                pageQuantity(userSearch.pageItensNumber!!, totalAmount)
            } else {
                pageQuantity(10, totalAmount)
            }
            val result = UserBResultSearch()
            result.list = list
            result.totalAmount = totalAmount
            result.pageQuantity = pageQuantity
            result
        }
    }

    fun saveBackofficeRole(backofficeRole: BackofficeRole) {
        if (backofficeRole.id == null) {
            backofficeRoleDAO.insert(backofficeRole)
        } else {
            backofficeRoleDAO.update(backofficeRole)
        }
    }

    private fun totalAmount(sb: SearchBuilder): Long {
        return userBDAO.count(sb.build())
    }

    private fun pageQuantity(numberOfItensByPage: Int, totalAmount: Long): Long {
        val pageQuantity: Long
        pageQuantity = if (totalAmount % numberOfItensByPage != 0L) {
            totalAmount / numberOfItensByPage + 1
        } else {
            totalAmount / numberOfItensByPage
        }
        return pageQuantity
    }

    fun search(userBSearch: UserBSearch): List<UserB?>? {
        val searchBuilder = userBDAO.createBuilder()
        if (userBSearch.roleID != null) {
            searchBuilder.appendParamQuery("role._id", userBSearch.roleID!!)
        }
        if (userBSearch.userBName != null) {
            searchBuilder.appendParamQuery("name", userBSearch.userBName!!, OperationEnum.LIKE)
        }
        if (userBSearch.userBDocument != null) {
            searchBuilder.appendParamQuery("document", userBSearch.userBDocument!!)
        }
        if (userBSearch.queryString != null && userBSearch.queryString!!.isNotEmpty()) {
            searchBuilder.appendParamQuery("name|nameObj|lastName|document", userBSearch.queryString!!, OperationEnum.OR_FIELDS_LIKE)
        }
        val returnList = ArrayList<UserB?>()
        userBDAO.search(searchBuilder.build())?.forEach { userB ->
            val newUserB = UserB()
            newUserB.id = userB.id
            newUserB.name = userB.name
            newUserB.lastName = userB.lastName
            newUserB.document = userB.document
            newUserB.status = userB.status
            returnList.add(newUserB)
        }
        return returnList
    }

    fun searchResponse(userBSearch: UserBSearch): ResponseList<UserB>? {
        val searchBuilder: SearchBuilder = userBDAO.createBuilder()
        if (userBSearch.roleID != null) {
            searchBuilder.appendParamQuery("role._id", userBSearch.roleID!!)
        }
        if (userBSearch.userBName != null) {
            searchBuilder.appendParamQuery("name", userBSearch.userBName!!, OperationEnum.LIKE)
        }
        if (userBSearch.userBDocument != null) {
            searchBuilder.appendParamQuery("document", userBSearch.userBDocument!!)
        }
        if (userBSearch.queryString != null && userBSearch.queryString!!.isNotEmpty()) {
            searchBuilder.appendParamQuery("name|nameObj|lastName|document", userBSearch.queryString!!, OperationEnum.OR_FIELDS_LIKE)
        }
        if (userBSearch.page == null) {
            userBSearch.page = 1
        }
        searchBuilder.setFirst(QUANTITY_PAGE * (userBSearch.page!! - 1))
        searchBuilder.setMaxResults(QUANTITY_PAGE)
        return userBDAO.searchToResponse(searchBuilder.build())
    }


    fun saveAvatar(file: FileUpload){
        val userb = retrieve(file.idObject) ?: throw BusinessException("user_not_foud")
        val url = bucketService.saveFile(file, userb.id!!, "userb", "image/jpg")
        userb.urlImage = url
        userBDAO.update(userb)
    }

    fun autoLogin(user: UserB): UserB? {
        val userDB = userBDAO.retrieve(UserB(user.id))
        if(!userAnonymous.toBoolean()){
            if (userDB == null || user.password == null) {
                throw BusinessException("user_not_found")
            }
            if (userDB.password == null || user.password != userDB.password) {
                throw BusinessException("invalid_user_password")
            }
            if (userDB.status != null && userDB.status == UserBStatusEnum.BLOCKED) {
                throw BusinessException("user_blocked")
            }
            if (userDB.token == null) {
                throw BusinessException("user_must_login_first")
            }
            createToken(userDB)
            return userDB
        } else {
            if (userDB == null) {
                throw BusinessException("user_not_found")
            }
            if (userDB.token == null) {
                throw BusinessException("user_no_token")
            }
            createToken(userDB)
            return userDB
        }
    }

    fun saveAnonymous(user: UserB): UserB? {
        if(!userAnonymous.toBoolean()){
            throw BusinessException("save_anonymous_not_supported")
        }
        if (user.id == null) {
            user.creationDate = Date()
            user.type = "anonymous"
            createToken(user)
            userBDAO.insert(user)
        }
        return user
    }
}