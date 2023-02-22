package com.orangebox.kit.admin.userb

import org.apache.commons.collections.CollectionUtils
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.annotation.PostConstruct
import javax.enterprise.inject.New
import javax.inject.Inject

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
class UserBServiceImpl : UserBService {
    @EJB
    private val configurationService: ConfigurationService? = null

    @EJB
    private val userAuthKeyService: UserAuthKeyService? = null

    @EJB
    private val notificationService: NotificationService? = null

    @Inject
    @New
    private val userBDAO: UserBDAO? = null

    @Inject
    @New
    private val backofficeRoleDAO: BackofficeRoleDAO? = null

    @EJB
    protected var tokenValidatorProvider: TokenValidatorProvider? = null
    private val QUANTITY_PAGE = 12
    @PostConstruct
    fun pos() {
        Logger.getLogger("org.mongodb.driver").level = Level.SEVERE
        Logger.getLogger("org.hibernate.search.reader.impl.ManagedMultiReader").level = Level.SEVERE
    }

    @Throws(Exception::class)
    override fun authenticateMobile(user: UserB?): UserB? {
        return authenticateMobile(user, user!!.password)
    }

    @Throws(Exception::class)
    private fun authenticateMobile(user: UserB?, password: String?): UserB {
        var userDB: UserB? = null
        userDB = retrieveByEmail(user!!.email)
        if (userDB == null) {
            throw BusinessException("user_not_found")
        }
        val passHash: String = SecUtils.generateHash(userDB.salt, password)
        if (userDB.password != passHash) {
            throw BusinessException("invalid_password")
        }
        userDB.token = UUID.randomUUID().toString()
        val expCal = Calendar.getInstance()
        expCal.add(Calendar.HOUR, 12)
        userDB.tokenExpirationDate = expCal.time
        userBDAO.update(userDB)
        if (userDB.idRole != null) {
            val role: BackofficeRole = backofficeRoleDAO.retrieve(BackofficeRole(userDB.idRole))
            userDB.setRole(role)
        }
        return if (userDB.status != UserBStatusEnum.BLOCKED) {
            userDB
        } else {
            throw BusinessException("User blocked")
        }
    }

    @Throws(Exception::class)
    override fun authenticate(user: UserB?): LoginInfo? {
        var info: LoginInfo? = null
        val userDB = authenticateMobile(user)
        info = LoginInfo()
        info.token = userDB!!.token
        info.userID = userDB.id
        info.idObj = userDB.idObj
        if (userDB.role != null && userDB.role.getRoles() != null) {
            val role: Role = userDB.role.getRoles().stream()
                .filter { p -> p.getFgHome() != null && p.getFgHome() }
                .findFirst()
                .orElse(null)
            if (role != null) {
                info.urlHome = role.getUrl()
            }
        }
        return if (userDB.status != UserBStatusEnum.BLOCKED) {
            info
        } else {
            throw BusinessException("User blocked")
        }
    }

    @Throws(Exception::class)
    private fun createToken(userDB: UserB) {
        userDB.token = UUID.randomUUID().toString()
        val expCal = Calendar.getInstance()
        expCal.add(Calendar.HOUR, 12)
        userDB.tokenExpirationDate = expCal.time
        userBDAO.update(userDB)
    }

    @Throws(Exception::class)
    override fun checkToken(token: String?): Boolean? {
        return tokenValidatorProvider.getTokenValidator().checkToken(token)
    }

    @Throws(Exception::class)
    override fun checkAcessRole(token: String?, url: String?): Boolean? {
        var validated = false
        val user = retrieveByToken(token)
        if (user!!.role != null && user.role.getRoles() != null) {
            for (role in user.role.getRoles()) {
                if (url.indexOf(role.getUrl()) != -1) {
                    validated = true
                    break
                }
            }
        }
        return validated
    }

    @Throws(Exception::class)
    override fun createNewUser(user: UserB?): UserB? {
        var userDB: UserB? = null
        if (user!!.email != null && user.email != "") {
            userDB = retrieveByEmail(user.email)
        }
        if (userDB != null) {
            throw BusinessException("email_already_registred")
        }
        if (user.status == null) {
            user.status = UserBStatusEnum.ACTIVE
        }
        if (user.password != null) {
            user.salt = SecUtils.getSalt()
            user.password = SecUtils.generateHash(user.salt, user.password)
        }
        user.creationDate = Date()
        if (user.role != null) {
            user.setRole(retrieveRole(user.role.getId()))
        }
        userBDAO.insert(user)
        val key: UserAuthKey = userAuthKeyService.createKey(user.id, UserAuthKeyTypeEnum.EMAIL)
        if (user.language == null) {
            user.language = "pt_BR"
        }
        val projectNameConf: Configuration = configurationService.loadByCode("PROJECT_NAME")
        var projectName = "Project"
        if (projectNameConf != null) {
            projectName = projectNameConf.getValue()
        }
        val title: String = MessageUtils.message(user.language, "user.confirm.email.title", projectName)
        val idEmailConfe: Configuration = configurationService.loadByCode("USER_BACKOFFICE_EMAIL_CONFIRM_ID")
        if (idEmailConfe != null && user.password == null) {
            val templateId: Int = configurationService.loadByCode("USER_BACKOFFICE_EMAIL_CONFIRM_ID").getValueAsInt()
            val msg = "definir"
            val link: String = configurationService.loadByCode("USER_BACKOFFICE_EMAIL_CONFIRM_LINK").getValue()
                .replaceAll("__LANGUAGE__", user.language!!.substring(1))
                .replaceAll("__KEY__", key.getKey())
                .replaceAll("__USER__", user.id)
                .replaceAll("__TYPE__", key.getType().toString())
            notificationService.sendNotification(
                NotificationBuilder()
                    .setTo(user)
                    .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                    .setTitle(title)
                    .setFgAlertOnly(true)
                    .setEmailDataTemplate(object : EmailDataTemplate() {
                        val data: Map<String, Any?>
                            get() {
                                val params: MutableMap<String, Any?> = HashMap()
                                params["user_name"] = user.name
                                params["msg"] = msg
                                params["confirmation_link"] = link
                                return params
                            }
                    })
                    .build()
            )
        }
        return userDB
    }

    @Throws(Exception::class)
    override fun updateUser(user: UserB?) {
        var userDBEmail: UserB? = null
        if (user!!.email != null && user.email != "") {
            userDBEmail = retrieveByEmail(user.email)
        }
        if (userDBEmail != null && userDBEmail.id != user.id) {
            throw BusinessException("email_already_registred")
        }
        val userDB: UserB = userBDAO.retrieve(UserB(user.id))
        if (user.emailConfirmed != null) {
            userDB.emailConfirmed = user.emailConfirmed
        }
        if (user.phoneConfirmed != null) {
            userDB.phoneConfirmed = user.phoneConfirmed
        }
        if (user.userConfirmed != null) {
            userDB.userConfirmed = user.userConfirmed
        }
        if (user.color != null) {
            userDB.color = user.color
        }
        if (user.creationDate != null) {
            userDB.creationDate = user.creationDate
        }
        if (user.email != null) {
            userDB.email = user.email
        }
        if (user.info != null) {
            userDB.setInfo(user.info)
        }
        if (user.keyAndroid != null) {
            userDB.keyAndroid = user.keyAndroid
        }
        if (user.keyIOS != null) {
            userDB.keyIOS = user.keyIOS
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
        if (user.role != null) {
            val role: BackofficeRole = backofficeRoleDAO.retrieve(user.role)
            userDB.setRole(role)
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
        userBDAO.update(userDB)
    }

    @Throws(Exception::class)
    override fun updatePassword(user: UserB?) {
        val userBase = authenticateMobile(user, user!!.oldPassword)
        if (userBase != null) {
            userBase.salt = SecUtils.getSalt()
            userBase.password = SecUtils.generateHash(userBase.salt, user.password)
            userBDAO.update(userBase)
        }
    }

    @Deprecated("")
    @Throws(Exception::class)
    override fun updatePasswordForgot(user: UserB?) {
        val userBase: UserB = userBDAO.retrieve(user)
        if (userBase != null) {
            userBase.salt = SecUtils.getSalt()
            userBase.password = SecUtils.generateHash(userBase.salt, user!!.password)
            userBDAO.update(userBase)
        }
    }

    @Throws(Exception::class)
    override fun saveUser(user: UserB?) {
        if (user!!.id == null) {
            createNewUser(user)
        } else {
            updateUser(user)
        }
    }

    @Throws(Exception::class)
    override fun retrieve(id: String?): UserB? {
        return userBDAO.retrieve(UserB(id))
    }

    @Throws(Exception::class)
    override fun retrieveByEmail(email: String?): UserB? {
        var user: UserB? = null
        val params: MutableMap<String, Any?> = HashMap()
        params["email"] = email
        user = userBDAO.retrieve(params)
        return user
    }

    @Throws(Exception::class)
    override fun retrieveByPhone(phoneNumber: Long?): UserB? {
        return userBDAO.retrieve(
            userBDAO.createBuilder()
                .appendParamQuery("phoneNumber", phoneNumber)
                .build()
        )
    }

    @Throws(Exception::class)
    override fun retrieveByEmailDetach(email: String?): UserB? {
        //nesta versao nao temos jpa, entao nao sera necessario efetuar o detach
        return retrieveByEmail(email)
    }

    @Throws(Exception::class)
    override fun retrieveByToken(token: String?): UserB? {
        var user: UserB? = null
        val params: MutableMap<String, Any?> = HashMap()
        params["token"] = token
        user = userBDAO.retrieve(params)
        if (user != null && user.idRole != null) {
            val role: BackofficeRole = backofficeRoleDAO.retrieve(BackofficeRole(user.idRole))
            user.setRole(role)
        }
        return user
    }

    @Throws(Exception::class)
    override fun retrieveRole(id: String?): BackofficeRole? {
        return backofficeRoleDAO.retrieve(BackofficeRole(id))
    }

    @Throws(Exception::class)
    override fun userCard(idUser: String?): UserBCard? {
        var userCard: UserBCard? = null
        val user = retrieve(idUser)
        userCard = userCard(user)
        return userCard
    }

    @Throws(Exception::class)
    override fun userCard(user: UserB?): UserBCard? {
        var userCard: UserBCard? = null
        userCard = UserBCard()
        userCard.id = user!!.id
        userCard.name = user.name
        return userCard
    }

    @Throws(Exception::class)
    override fun listAdminUsers(): List<UserB?>? {
        var list: List<UserB?>? = null
        val params: MutableMap<String, Any> = HashMap()
        params["role.fgAdmin"] = true
        list = userBDAO.search(params)
        return list
    }

    @Throws(Exception::class)
    override fun listUsersByRole(roleId: String?): List<UserB?>? {
        var list: List<UserB?>? = null
        val params: MutableMap<String, Any?> = HashMap()
        params["role.id"] = roleId
        list = userBDAO.search(params)
        return list
    }

    @Deprecated("")
    @Throws(Exception::class)
    override fun changeStatus(idUserB: String?) {
        val userB = retrieve(idUserB)
        if (userB!!.status == UserBStatusEnum.ACTIVE) {
            userB.status = UserBStatusEnum.BLOCKED
        } else {
            userB.status = UserBStatusEnum.ACTIVE
        }
        userBDAO.update(userB)
    }

    @Throws(Exception::class)
    override fun createFirstAdminUser() {
        var userB = retrieveByEmail("admin@mangobits.com")
        if (userB == null) {
            userB = UserB()
            userB.email = "admin@mangobits.com"
            userB.name = "Admin MangoBits"
            userB.password = "admin"
            userB.setRole(retrieveRole("admin"))
            saveUser(userB)
        }
    }

    @Throws(Exception::class)
    override fun listAdminRoles(): List<BackofficeRole?>? {
        var list: List<BackofficeRole?>? = null
        val params: MutableMap<String, Any> = HashMap()
        params["fgAdmin"] = true
        list = backofficeRoleDAO.search(params)
        return list
    }

    @Throws(Exception::class)
    override fun listOperationalRoles(): List<BackofficeRole?>? {
        var list: List<BackofficeRole?>? = null
        val params: MutableMap<String, Any> = HashMap()
        params["fgAdmin"] = false
        list = backofficeRoleDAO.search(params)
        return list
    }

    override fun listAllRoles(): List<BackofficeRole?>? {
        return backofficeRoleDAO.listAll()
    }

    @Throws(Exception::class)
    override fun forgotPassword(email: String?) {
        val user = retrieveByEmail(email) ?: throw BusinessException("user_not_found")
        val key: UserAuthKey = userAuthKeyService.createKey(user.id, UserAuthKeyTypeEnum.EMAIL)
        var language = user.language
        if (language == null) {
            language = "pt_BR"
            user.language = language
        }
        val projectName: String = configurationService.loadByCode("PROJECT_NAME").getValue()
        val title: String = MessageUtils.message(
            LanguageEnum.localeByLanguage(language),
            "user.confirm.email.forgot.title",
            projectName
        )
        val templateId: Int = configurationService.loadByCode("USER_BACKOFFICE_EMAIL_FORGET_ID").getValueAsInt()
        val msg = "mudar"
        val projectLogo: String = configurationService.loadByCode("PROJECT_LOGO_URL").getValue()
        val baseLink = "__BASE__/email_forgot_password_userb?l=__LANGUAGE__&k=__KEY__&u=__USER__&t=__TYPE__"
        val link: String = baseLink
            .replace("__BASE__".toRegex(), configurationService.loadByCode("PROJECT_URL").getValue())
            .replace("__LANGUAGE__".toRegex(), user.language!!.substring(1))
            .replace("__KEY__".toRegex(), key.getKey())
            .replace("__USER__".toRegex(), user.id)
            .replace("__TYPE__".toRegex(), key.getType().toString())
        notificationService.sendNotification(
            NotificationBuilder()
                .setTo(user)
                .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                .setTitle(title)
                .setFgAlertOnly(true)
                .setEmailDataTemplate(object : EmailDataTemplate() {
                    val data: Map<String, Any?>
                        get() {
                            val params: MutableMap<String, Any?> = HashMap()
                            params["user_name"] = user.name
                            params["msg"] = msg
                            params["confirmation_link"] = link
                            params["project_name"] = projectName
                            params["project_logo"] = projectLogo
                            return params
                        }
                })
                .build()
        )
    }

    @Throws(Exception::class)
    override fun validateKey(key: UserAuthKey?): Boolean? {
        var validate = false
        validate = userAuthKeyService.validateKey(key)
        if (validate) {
            val user: UserB = userBDAO.retrieve(UserB(key.getIdUser()))
            user.userConfirmed = true
            if (key.getType().equals(UserAuthKeyTypeEnum.EMAIL)) {
                user.emailConfirmed = true
            } else {
                user.phoneConfirmed = true
            }
            userBDAO.update(user)
        }
        return validate
    }

    @Throws(Exception::class)
    override fun retrieveByOfficeRole(idOffice: String?, idBackofficeRole: String?): UserB? {
        var user: UserB? = null
        val listUserB = userBDAO!!.listByOfficeRole(idOffice, idBackofficeRole)
        if (CollectionUtils.isNotEmpty(listUserB)) {
            user = listUserB[0]
        }
        return user
    }

    @Throws(Exception::class)
    override fun listByOfficeRole(idOffice: String?, idBackofficeRole: String?): List<UserB?>? {
        return userBDAO!!.listByOfficeRole(idOffice, idBackofficeRole)
    }

    @Throws(Exception::class)
    override fun listByOfficeRoleDepartment(
        idOffice: String?,
        idBackofficeRole: String?,
        idDepartment: String?
    ): List<UserB?>? {
        return userBDAO!!.listByOfficeRoleDepartment(idOffice, idBackofficeRole, idDepartment)
    }

    @Throws(Exception::class)
    override fun retrieveByOfficeRoleDepartment(
        idOffice: String?,
        idBackofficeRole: String?,
        idDepartment: String?
    ): UserB? {
        var user: UserB? = null
        val listUserB = userBDAO!!.listByOfficeRoleDepartment(idOffice, idBackofficeRole, idDepartment)
        if (CollectionUtils.isNotEmpty(listUserB)) {
            user = listUserB[0]
        }
        return user
    }

    @Throws(Exception::class)
    override fun pathFiles(idUser: String?): String? {
        return configurationService.loadByCode(ConfigurationEnum.PATH_BASE).getValue() + "/userb/" + idUser
    }

    @Throws(Exception::class)
    override fun sendNotificationAppByBackofficeRole(backofficeRoleEnum: BackofficeRoleEnum?, message: String?) {
        val listUserB = listUsersByRole(backofficeRoleEnum.getId())
        if (CollectionUtils.isNotEmpty(listUserB)) {
            for (userB in listUserB!!) {
                sendNotification(userB, message)
            }
        }
    }

    @Throws(Exception::class)
    fun sendNotification(userB: UserB?, message: String?) {
        notificationService.sendNotification(
            NotificationBuilder()
                .setTo(userB)
                .setTypeSending(TypeSendingNotificationEnum.APP)
                .setMessage(message)
                .build()
        )
    }

    @Throws(Exception::class)
    override fun searchAdmin(userSearch: UserBSearch?): UserBResultSearch? {
        return if (userSearch!!.page == null) {
            throw BusinessException("missing_page")
        } else {
            val sb: SearchBuilder = userBDAO.createBuilder()
            if (userSearch.status != null) {
                sb.appendParamQuery("status", userSearch.status)
            }
            if (userSearch.idObj != null) {
                sb.appendParamQuery("idObj", userSearch.idObj)
            }
            if (userSearch.queryString != null && !userSearch.queryString!!.isEmpty()) {
                sb.appendParamQuery("name|nameObj", userSearch.queryString, OperationEnum.OR_FIELDS)
            }
            if (userSearch.type != null) {
                sb.appendParamQuery("type", userSearch.type)
            }
            if (userSearch.pageItensNumber != null && userSearch.pageItensNumber!! > 0) {
                sb.setFirst(userSearch.pageItensNumber!! * (userSearch.page!! - 1))
                sb.setMaxResults(userSearch.pageItensNumber)
            } else {
                sb.setFirst(10 * (userSearch.page!! - 1))
                sb.setMaxResults(10)
            }
            sb.appendSort("name", 1)
            val list: List<UserB> = userBDAO.search(sb.build())
            val totalAmount = totalAmount(sb)
            val pageQuantity: Long
            pageQuantity = if (userSearch.pageItensNumber != null && userSearch.pageItensNumber!! > 0) {
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

    @Throws(Exception::class)
    override fun saveBackofficeRole(backofficeRole: BackofficeRole?) {
        if (backofficeRole.getId() == null) {
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

    override fun search(userBSearch: UserBSearch?): List<UserB?>? {
        val searchBuilder: SearchBuilder = userBDAO.createBuilder()
        if (userBSearch!!.roleID != null) {
            searchBuilder.appendParamQuery("role._id", userBSearch.roleID)
        }
        if (userBSearch.userBName != null) {
            searchBuilder.appendParamQuery("name", userBSearch.userBName, OperationEnum.LIKE)
        }
        if (userBSearch.userBDocument != null) {
            searchBuilder.appendParamQuery("document", userBSearch.userBDocument)
        }
        if (userBSearch.queryString != null) {
            searchBuilder.appendParamQuery("name|document", userBSearch.queryString, OperationEnum.OR_FIELDS_LIKE)
        }
        val userBList: List<UserB> = userBDAO.search(searchBuilder.build())
        val returnList = ArrayList<UserB?>()
        for (userB in userBList) {
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

    override fun searchResponse(userBSearch: UserBSearch?): ResponseList<UserB> {
        val searchBuilder: SearchBuilder = userBDAO.createBuilder()
        if (userBSearch!!.roleID != null) {
            searchBuilder.appendParamQuery("role._id", userBSearch.roleID)
        }
        if (userBSearch.userBName != null) {
            searchBuilder.appendParamQuery("name", userBSearch.userBName, OperationEnum.LIKE)
        }
        if (userBSearch.userBDocument != null) {
            searchBuilder.appendParamQuery("document", userBSearch.userBDocument)
        }
        if (userBSearch.queryString != null) {
            searchBuilder.appendParamQuery("name|document", userBSearch.queryString, OperationEnum.OR_FIELDS_LIKE)
        }
        if (userBSearch.page == null) {
            userBSearch.page = 1
        }
        searchBuilder.setFirst(QUANTITY_PAGE * (userBSearch.page!! - 1))
        searchBuilder.setMaxResults(QUANTITY_PAGE)
        val userBList: ResponseList<UserB> = userBDAO.searchToResponse(searchBuilder.build())
        val returnList: ResponseList<UserB> = ResponseList()
        val returnUserBList = ArrayList<UserB>()
        for (userB in userBList.getList()) {
            val newUserB = UserB()
            newUserB.id = userB.id
            newUserB.name = userB.name
            newUserB.lastName = userB.lastName
            newUserB.document = userB.document
            newUserB.status = userB.status
            returnUserBList.add(newUserB)
        }
        returnList.setList(returnUserBList)
        returnList.setPageQuantity(userBList.getPageQuantity())
        returnList.setTotalAmount(userBList.getTotalAmount())
        return returnList
    }
}