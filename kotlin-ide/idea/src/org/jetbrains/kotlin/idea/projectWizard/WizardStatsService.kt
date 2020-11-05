/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.projectWizard

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomValidationRule
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfoById
import org.jetbrains.kotlin.idea.KotlinPluginUtil

interface WizardStats {
    fun toPairs(): ArrayList<EventPair<*>>
}

class WizardStatsService : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    companion object {

        // Collector ID
        private val GROUP = EventLogGroup("kotlin.ide.new.project", 2)

        // Whitelisted values for the events fields
        private val allowedProjectTemplates = listOf( // Modules
            "JVM_|_IDEA",
            "JS_|_IDEA",
            // Java and Gradle groups
            "Kotlin/JVM",
            // Gradle group
            "Kotlin/JS",
            "Kotlin/JS_for_browser",
            "Kotlin/JS_for_Node.js",
            "Kotlin/Multiplatform_as_framework",
            "Kotlin/Multiplatform",
            // Kotlin group
            "backendApplication",
            "consoleApplication",
            "multiplatformMobileApplication",
            "multiplatformMobileLibrary",
            "multiplatformApplication",
            "multiplatformLibrary",
            "nativeApplication",
            "frontendApplication",
            "fullStackWebApplication",
            "nodejsApplication",
            "composeDesktopApplication",
            "composeMultiplatformApplication",
            "none"
        )
        private val allowedModuleTemplates = listOf(
            "composeAndroid",
            "composeDesktopTemplate",
            "composeMppModule",
            "consoleJvmApp",
            "ktorServer",
            "mobileMppModule",
            "nativeConsoleApp",
            "reactJsClient",
            "simpleJsClient",
            "simpleNodeJs",
            "none",
        )

        private val allowedWizardsGroups = listOf("Java", "Kotlin", "Gradle")
        private val allowedBuildSystems = listOf(
            "gradleKotlin",
            "gradleGroovy",
            "jps",
            "maven"
        )

        // Event fields
        val groupField = EventFields.String("group", allowedWizardsGroups)
        val projectTemplateField = EventFields.String("project_template", allowedProjectTemplates)
        val buildSystemField = EventFields.String("build_system", allowedBuildSystems)

        val modulesCreatedField = EventFields.Int("modules_created")
        val modulesRemovedField = EventFields.Int("modules_removed")
        val moduleTemplateChangedField = EventFields.Int("module_template_changed")

        val moduleTemplateField = EventFields.String("module_template", allowedModuleTemplates)

        private val pluginInfoField = EventFields.PluginInfo.with(getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID))

        // Events
        private val projectCreatedEvent = GROUP.registerVarargEvent(
            "project_created",
            groupField,
            projectTemplateField,
            buildSystemField,
            modulesCreatedField,
            modulesRemovedField,
            moduleTemplateChangedField,
            EventFields.PluginInfo
        )

        private val projectOpenedByHyperlinkEvent = GROUP.registerVarargEvent(
            "wizard_opened_by_hyperlink",
            projectTemplateField,
            EventFields.PluginInfo
        )

        private val moduleTemplateCreatedEvent = GROUP.registerVarargEvent(
            "module_template_created",
            projectTemplateField,
            moduleTemplateField,
            EventFields.PluginInfo
        )

        // Log functions
        fun logDataOnProjectGenerated(projectCreationStats: ProjectCreationStats) {
            projectCreatedEvent.log(
                *projectCreationStats.toPairs().toTypedArray(),
                pluginInfoField
            )
        }

        fun logDataOnProjectGenerated(projectCreationStats: ProjectCreationStats, uiEditorUsageStats: UiEditorUsageStats) {
            projectCreatedEvent.log(
                *projectCreationStats.toPairs().toTypedArray(),
                *uiEditorUsageStats.toPairs().toTypedArray(),
                pluginInfoField
            )
        }

        fun logUsedModuleTemplatesOnNewWizardProjectCreated(projectTemplateId: String, moduleTemplates: List<String>) {
            moduleTemplates.forEach { moduleTemplateId ->
                logModuleTemplateCreation(projectTemplateId, moduleTemplateId)
            }
        }

        fun logWizardOpenByHyperlink(templateId: String?) {
            projectOpenedByHyperlinkEvent.log(
                projectTemplateField.with(templateId ?: "none"),
                pluginInfoField
            )
        }

        private fun logModuleTemplateCreation(projectTemplateId: String, moduleTemplateId: String) {
            moduleTemplateCreatedEvent.log(
                projectTemplateField.with(projectTemplateId),
                moduleTemplateField.with(moduleTemplateId),
                pluginInfoField
            )
        }
    }

    data class ProjectCreationStats(
        val group: String,
        val projectTemplateId: String,
        val buildSystemType: String,
    ) : WizardStats {
        override fun toPairs(): ArrayList<EventPair<*>> = arrayListOf(
            groupField.with(group),
            projectTemplateField.with(projectTemplateId),
            buildSystemField.with(buildSystemType)
        )
    }

    data class UiEditorUsageStats(
        var modulesCreated: Int = 0,
        var modulesRemoved: Int = 0,
        var moduleTemplateChanged: Int = 0
    ) : WizardStats {
        override fun toPairs(): ArrayList<EventPair<*>> = arrayListOf(
            modulesCreatedField.with(modulesCreated),
            modulesRemovedField.with(modulesRemoved),
            moduleTemplateChangedField.with(moduleTemplateChanged)
        )
    }
}

