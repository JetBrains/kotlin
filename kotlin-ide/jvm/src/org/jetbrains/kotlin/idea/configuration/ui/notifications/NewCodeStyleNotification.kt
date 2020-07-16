/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.application.options.CodeStyle
import com.intellij.facet.ProjectFacetManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.idea.formatter.kotlinCodeStyleDefaults

private const val KOTLIN_UPDATE_CODE_STYLE_GROUP_ID = "Update Kotlin code style"
private const val KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME = "update.kotlin.code.style.notified"

fun notifyKotlinStyleUpdateIfNeeded(project: Project) {
    val modulesWithFacet = ProjectFacetManager.getInstance(project).getModulesWithFacet(KotlinFacetType.TYPE_ID)
    if (modulesWithFacet.isEmpty()) return

    if (CodeStyle.getSettings(project).kotlinCodeStyleDefaults() == KotlinStyleGuideCodeStyle.CODE_STYLE_ID) return

    val settingsComponent = PropertiesComponent.getInstance(project)
    if (settingsComponent.getBoolean(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, false)) {
        return
    }

    val notification = createNotification()
    NotificationsConfiguration.getNotificationsConfiguration().register(
        KOTLIN_UPDATE_CODE_STYLE_GROUP_ID,
        NotificationDisplayType.STICKY_BALLOON,
        true
    )

    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }

    settingsComponent.setValue(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, true, false)

    notification.notify(project)
}

private fun createNotification(): Notification = Notification(
    KOTLIN_UPDATE_CODE_STYLE_GROUP_ID,
    KotlinJvmBundle.message("kotlin.code.style"),
    KotlinJvmBundle.htmlMessage("notification.update.code.style.to.official"),
    NotificationType.WARNING
).apply {
    val notificationAction = NotificationAction.create(
        KotlinJvmBundle.message("apply.new.code.style")
    ) { e: AnActionEvent, notification: Notification ->
        notification.expire()

        e.project?.takeIf { !it.isDisposed }?.let { project ->
            runWriteAction {
                KotlinStyleGuideCodeStyle.apply(CodeStyle.getSettings(project))
            }
        }
    }

    addAction(notificationAction)
    isImportant = true
}