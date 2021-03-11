/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.configuration.getConfigurationPossibilitiesForConfigureNotification
import org.jetbrains.kotlin.idea.configuration.getConfiguratorByName
import org.jetbrains.kotlin.idea.configuration.ui.KotlinConfigurationCheckerService
import javax.swing.event.HyperlinkEvent

data class ConfigureKotlinNotificationState(
    val debugProjectName: String,
    val notificationString: String,
    val notConfiguredModules: Collection<String>
)

class ConfigureKotlinNotification(
    project: Project,
    excludeModules: List<Module>,
    val notificationState: ConfigureKotlinNotificationState
) : Notification(
    KotlinConfigurationCheckerService.CONFIGURE_NOTIFICATION_GROUP_ID,
    KotlinJvmBundle.message("configure.kotlin"),
    notificationState.notificationString,
    NotificationType.WARNING,
    NotificationListener { notification, event ->
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            val configurator = getConfiguratorByName(event.description) ?: throw AssertionError("Missed action: " + event.description)
            notification.expire()

            configurator.configure(project, excludeModules)
        }
    }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConfigureKotlinNotification) return false

        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        return content.hashCode()
    }

    companion object {
        fun getNotificationState(project: Project, excludeModules: Collection<Module>): ConfigureKotlinNotificationState? {
            val (configurableModules, ableToRunConfigurators) = getConfigurationPossibilitiesForConfigureNotification(
                project,
                excludeModules
            )
            if (ableToRunConfigurators.isEmpty() || configurableModules.isEmpty()) return null

            val isOnlyOneModule = configurableModules.size == 1

            val modulesString = if (isOnlyOneModule)
                KotlinJvmBundle.message("configure.0.module", configurableModules.first().baseModule.name)
            else
                KotlinJvmBundle.message("configure.modules")
            val links = ableToRunConfigurators.joinToString(separator = "<br/>") { configurator ->
                getLink(configurator, isOnlyOneModule)
            }

            return ConfigureKotlinNotificationState(
                project.name,
                KotlinJvmBundle.message("configure.0.in.1.project.br.2", modulesString, project.name, links),
                configurableModules.map { it.baseModule.name }
            )
        }

        private fun getLink(configurator: KotlinProjectConfigurator, isOnlyOneModule: Boolean): String =
            "<a href=\"${configurator.name}\">${KotlinJvmBundle.message(
                "as.kotlin.1.module",
                configurator.presentableText,
                1.takeIf { isOnlyOneModule } ?: 2
            )}</a>"
    }
}
