package com.orangebox.kit.admin.userb

import com.orangebox.kit.admin.role.BackofficeRole
import com.orangebox.kit.core.annotation.OKEntity
import com.orangebox.kit.core.annotation.OKId
import com.orangebox.kit.core.user.GeneralUser
import jakarta.json.bind.annotation.JsonbTransient
import java.util.*

@OKEntity(name = "userB")
class UserB: GeneralUser {

	@OKId
	override var id: String? = null

	override var phoneNumber: Long? = null

	override var phoneCountryCode: Int? = null

	override var email: String? = null

	override var tokenFirebase: String? = null

	var idObj: String? = null

    var idRole: String? = null

	override var name: String? = null

	var username: String? = null

	override var lastName: String? = null

	var document: String? = null

	var password: String? = null

	var oldPassword: String? = null

	@JsonbTransient
    var salt: String? = null

    var creationDate: Date? = null

	var status: UserBStatusEnum? = null

	var token: String? = null

    var type: String? = null

    var tokenExpirationDate: Date? = null

	var userConfirmed: Boolean? = null

	var emailConfirmed: Boolean? = null

	var phoneConfirmed: Boolean? = null

	var role: BackofficeRole? = null

	var info: HashMap<String, Any>? = null

    var lastLogin: Date? = null

	var language: String? = null

	override var urlImage: String? = null

	var respRecaptcha: String? = null

	var appId: String? = null

    constructor()
    constructor(id: String?) {
        this.id = id
    }
}