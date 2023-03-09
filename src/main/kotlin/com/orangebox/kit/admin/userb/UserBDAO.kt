package com.orangebox.kit.admin.userb

import com.orangebox.kit.core.dao.AbstractDAO

class UserBDAO : AbstractDAO<UserB>(UserB::class.java) {
    override fun getId(user: UserB): Any? {
        return user.id
    }

    fun listByInfo(infoKey: String, infoValue: String): List<UserB>? {
        return search(
            createBuilder()
                .appendParamQuery("info.$infoKey", infoValue)
                .build()
        )
    }

    fun listByOfficeRole(idOffice: String, idBackofficeRole: String): List<UserB>? {
        return search(
            createBuilder()
                .appendParamQuery("info.idOffice", idOffice)
                .appendParamQuery("role_id", idBackofficeRole)
                .build()
        )
    }

    fun listByOfficeRoleDepartment(idOffice: String, idBackofficeRole: String, idDepartment: String): List<UserB>? {
        return search(
            createBuilder()
                .appendParamQuery("info.idOffice", idOffice)
                .appendParamQuery("role_id", idBackofficeRole)
                .build()
        )
    }
}