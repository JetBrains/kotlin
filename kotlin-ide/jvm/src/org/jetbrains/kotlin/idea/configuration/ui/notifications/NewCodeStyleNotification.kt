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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.formatter.*
import org.jetbrains.kotlin.idea.util.isDefaultIntellijOrObsoleteCodeStyle

private const val KOTLIN_UPDATE_CODE_STYLE_GROUP_ID = "Update Kotlin code style"
private const val KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME = "update.kotlin.code.style.notified"

fun notifyKotlinStyleUpdateIfNeeded(project: Project) {
    val modulesWithFacet = ProjectFacetManager.getInstance(project).getModulesWithFacet(KotlinFacetType.TYPE_ID)
    if (modulesWithFacet.isEmpty()) return
    val codeStyle = CodeStyle.getSettings(project)
    if (!codeStyle.kotlinCommonSettings.isDefaultIntellijOrObsoleteCodeStyle) return

    val isProjectSettings = CodeStyle.usesOwnSettings(project)
    val settingsComponent: PropertiesComponent = if (isProjectSettings) {
        PropertiesComponent.getInstance(project)
    } else {
        PropertiesComponent.getInstance()
    }

    if (settingsComponent.getBoolean(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, false)) {
        return
    }

    val notification = KotlinCodeStyleChangedNotification.create(project, isProjectSettings) ?: return
    notification.isImportant = true

    NotificationsConfiguration.getNotificationsConfiguration()
        .register(KOTLIN_UPDATE_CODE_STYLE_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true)

    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }

    settingsComponent.setValue(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, true, false)

    notification.notify(project)
}

private class KotlinCodeStyleChangedNotification(val project: Project, isProjectSettings: Boolean) : Notification(
    KOTLIN_UPDATE_CODE_STYLE_GROUP_ID,
    KotlinJvmBundle.message("kotlin.code.style"),
    KotlinJvmBundle.htmlMessage("default.code.style.was.updated.to.kotlin.coding.conventions"),
    NotificationType.WARNING,
    null
) {
    init {
        val ktFormattingSettings = ktCodeStyleSettings(project)

        if (isProjectSettings) {
            addAction(object : NotificationAction(KotlinJvmBundle.message("apply.new.code.style")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()

                    val ktSettings = ktCodeStyleSettings(project) ?: return

                    runWriteAction {
                        KotlinStyleGuideCodeStyle.apply(ktSettings.all)
                    }
                }
            })
        }

        if (ktFormattingSettings != null && ktFormattingSettings.canRestore()) {
            addAction(object : NotificationAction(KotlinJvmBundle.message("restore.old.settings")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()

                    val ktSettings = ktCodeStyleSettings(project) ?: return

                    runWriteAction {
                        ktSettings.restore()
                    }
                }
            })
        }
    }

    companion object {
        val LOG = Logger.getInstance("KotlinCodeStyleChangedNotification")

        fun create(project: Project, isProjectSettings: Boolean): KotlinCodeStyleChangedNotification? {
            val ktFormattingSettings = ktCodeStyleSettings(project) ?: return null

            if (isProjectSettings && !ktFormattingSettings.hasDefaultLoadScheme()) {
                return null
            }

            return KotlinCodeStyleChangedNotification(project, isProjectSettings)
        }
    }
}