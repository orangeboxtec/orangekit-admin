package com.orangebox.kit.admin.role

import com.orangebox.kit.core.dao.AbstractDAO
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BackofficeRoleDAO: AbstractDAO<BackofficeRole>(BackofficeRole::class.java) {
    override fun getId(bean: BackofficeRole): String? {
        return bean.id
    }
}