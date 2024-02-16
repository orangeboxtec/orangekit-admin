package com.orangebox.kit.admin.userb

import com.orangebox.kit.core.dao.AbstractDAO
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserBDAO : AbstractDAO<UserB>(UserB::class.java) {
    override fun getId(bean: UserB): Any? {
        return bean.id
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