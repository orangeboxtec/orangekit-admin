package com.orangebox.kit.admin.userb

import com.orangebox.kit.admin.role.BackofficeRole
import com.orangebox.kit.core.annotation.OKEntity
import com.orangebox.kit.core.annotation.OKId
import java.util.*

@OKEntity(name = "userB")
class UserB {

	@OKId
    var id: String? = null

	var idObj: String? = null

    var idRole: String? = null

	var email: String? = null

	var name: String? = null

	var lastName: String? = null

	var document: String? = null

	var password: String? = null

	var oldPassword: String? = null

    var salt: String? = null

    var creationDate: Date? = null

	var status: UserBStatusEnum? = null

	var token: String? = null

    var type: String? = null

	var keyIOS: String? = null

	var keyAndroid: String? = null

	var phoneNumber: Long? = null

	var phoneCountryCode: Int? = null

    var tokenExpirationDate: Date? = null

	var userConfirmed: Boolean? = null

	var emailConfirmed: Boolean? = null

	var phoneConfirmed: Boolean? = null

	var role: BackofficeRole? = null

	var info: Map<String, String>? = null

	var color: String? = null

    var lastAddress: AddressInfo? = null

    var lastLogin: Date? = null

	var language: String? = null

    constructor()
    constructor(id: String?) {
        this.id = id
    }
}