package com.orangebox.kit.admin.userb

import org.startupkit.admin.role.BackofficeRole

@Local
interface UserBService {
    @Throws(Exception::class)
    fun authenticate(userB: UserB?): LoginInfo?

    @Throws(Exception::class)
    fun authenticateMobile(user: UserB?): UserB?

    @Throws(Exception::class)
    fun checkToken(token: String?): Boolean?

    @Throws(Exception::class)
    fun checkAcessRole(token: String?, url: String?): Boolean?

    @Throws(Exception::class)
    fun createNewUser(userB: UserB?): UserB?

    @Throws(Exception::class)
    fun updateUser(userB: UserB?)

    @Throws(Exception::class)
    fun updatePassword(user: UserB?)

    @Throws(Exception::class)
    fun saveUser(userB: UserB?)

    @Deprecated("")
    @Throws(Exception::class)
    fun changeStatus(idUserB: String?)

    @Throws(Exception::class)
    fun retrieve(id: String?): UserB?

    @Throws(Exception::class)
    fun retrieveByEmail(email: String?): UserB?

    @Throws(Exception::class)
    fun retrieveByPhone(phoneNumber: Long?): UserB?

    @Throws(Exception::class)
    fun retrieveByEmailDetach(email: String?): UserB?

    @Throws(Exception::class)
    fun retrieveByToken(token: String?): UserB?

    @Throws(Exception::class)
    fun userCard(idUser: String?): UserBCard?

    @Throws(Exception::class)
    fun userCard(user: UserB?): UserBCard?

    @Throws(Exception::class)
    fun retrieveRole(id: String?): BackofficeRole?

    @Throws(Exception::class)
    fun listAdminUsers(): List<UserB?>?

    @Throws(Exception::class)
    fun listUsersByRole(roleId: String?): List<UserB?>?

    @Throws(Exception::class)
    fun createFirstAdminUser()

    @Throws(Exception::class)
    fun listAdminRoles(): List<BackofficeRole?>?

    @Throws(Exception::class)
    fun listOperationalRoles(): List<BackofficeRole?>?

    @Throws(Exception::class)
    fun forgotPassword(email: String?)

    @Throws(Exception::class)
    fun validateKey(key: UserAuthKey?): Boolean?

    @Throws(Exception::class)
    fun pathFiles(idUser: String?): String?

    @Deprecated("")
    @Throws(Exception::class)
    fun updatePasswordForgot(user: UserB?)

    @Throws(Exception::class)
    fun retrieveByOfficeRole(idOffice: String?, idBackofficeRole: String?): UserB?

    @Throws(Exception::class)
    fun retrieveByOfficeRoleDepartment(idOffice: String?, idBackofficeRole: String?, idDepartment: String?): UserB?

    @Throws(Exception::class)
    fun listByOfficeRole(idOffice: String?, idBackofficeRole: String?): List<UserB?>?

    @Throws(Exception::class)
    fun listByOfficeRoleDepartment(idOffice: String?, idBackofficeRole: String?, idDepartment: String?): List<UserB?>?

    @Throws(Exception::class)
    fun sendNotificationAppByBackofficeRole(backofficeRoleEnum: BackofficeRoleEnum?, message: String?)

    @Throws(BusinessException::class)
    fun listAllRoles(): List<BackofficeRole?>?

    @Throws(Exception::class)
    fun searchAdmin(userSearch: UserBSearch?): UserBResultSearch?

    @Throws(Exception::class)
    fun saveBackofficeRole(backofficeRole: BackofficeRole?)
    fun search(userBSearch: UserBSearch?): List<UserB?>?
    fun searchResponse(userBSearch: UserBSearch?): ResponseList<UserB?>?
}