package com.orangebox.kit.admin.role

import com.orangebox.kit.core.annotation.OKEntity
import com.orangebox.kit.core.annotation.OKId

@OKEntity(name = "backofficeRole")
class BackofficeRole {

    @OKId
    var id: String? = null

    var name: String? = null

    var fgAdmin: Boolean? = null

    var roles: List<Role>? = null

    constructor()
    constructor(id: String?) {
        this.id = id
    }
}