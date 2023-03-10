package com.orangebox.kit.admin.userb

import com.orangebox.kit.core.configuration.ConfigurationService
import io.quarkus.runtime.Startup
import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Startup
class OKAdminBoot {

    @Inject
    private lateinit var configurationService: ConfigurationService

    @PostConstruct
    fun run() {
        configurationService.checkAndSave("PROJECT_URL", "https://www.startupkit.cloud")
        configurationService.checkAndSave("PROJECT_LOGO_URL", "http://admin.startupkit.mangotest.com/img/mango.png")
        configurationService.checkAndSave("USER_BACKOFFICE_EMAIL_FORGET_ID", "1080189")
        val mailData: HashMap<String, String> = HashMap()
        mailData["host"] = "in-v3.mailjet.com"
        mailData["user"] = "CHANGE_IT"
        mailData["password"] = "CHANGE_IT"
        mailData["from"] = "hello@orangebox.technology"
        mailData["fromName"] = "Amazing Project"
        mailData["smtpHost"] = "in-v3.mailjet.com"
        mailData["smtpPort"] = "587"
        mailData["authorization"] = "true"
        configurationService.checkAndSave("MAIL_DATA", mailData)
    }
}