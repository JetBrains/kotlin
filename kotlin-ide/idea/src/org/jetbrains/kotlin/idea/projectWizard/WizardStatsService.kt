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
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import kotlin.math.abs
import kotlin.random.Random

interface WizardStats {
    fun toPairs(): ArrayList<EventPair<*>>
}

class WizardStatsService : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    companion object {

        // Collector ID
        private val GROUP = EventLogGroup("kotlin.ide.new.project", 4)

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
            "reactApplication",
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

        private val settings = Settings(
            SettingIdWithPossibleValues.Enum(
                id = "buildSystem.type",
                values = listOf(
                    "GradleKotlinDsl",
                    "GradleGroovyDsl",
                    "Jps",
                    "Maven",
                )
            ),

            SettingIdWithPossibleValues.Enum(
                id = "testFramework",
                values = listOf(
                    "NONE",
                    "JUNIT4",
                    "JUNIT5",
                    "TEST_NG",
                    "JS",
                    "COMMON",
                )
            ),

            SettingIdWithPossibleValues.Enum(
                id = "targetJvmVersion",
                values = listOf(
                    "JVM_1_6",
                    "JVM_1_8",
                    "JVM_9",
                    "JVM_10",
                    "JVM_11",
                    "JVM_12",
                    "JVM_13",
                )
            ),

            SettingIdWithPossibleValues.Enum(
                id = "androidPlugin",
                values = listOf(
                    "APPLICATION",
                    "LIBRARY",
                )
            ),

            SettingIdWithPossibleValues.Enum(
                id = "serverEngine",
                values = listOf(
                    "Netty",
                    "Tomcat",
                    "Jetty",
                )
            ),

            SettingIdWithPossibleValues.Enum(
                id = "kind",
                idToLog = "js.project.kind",
                values = listOf(
                    "LIBRARY",
                    "APPLICATION",
                )
            ),

            SettingIdWithPossibleValues.Enum(
                id = "compiler",
                idToLog = "js.compiler",
                values = listOf(
                    "IR",
                    "LEGACY",
                    "BOTH",
                )
            ),
            SettingIdWithPossibleValues.Enum(
                id = "projectTemplates.template",
                values = allowedProjectTemplates
            ),
            SettingIdWithPossibleValues.Enum(
                id = "module.template",
                values = allowedModuleTemplates
            ),
            SettingIdWithPossibleValues.Enum(
                id = "buildSystem.type",
                values = allowedBuildSystems
            ),

            SettingIdWithPossibleValues.Boolean(
                id = "javaSupport",
                idToLog = "jvm.javaSupport"
            ),
            SettingIdWithPossibleValues.Boolean(
                id = "cssSupport",
                idToLog = "js.cssSupport"
            ),
            SettingIdWithPossibleValues.Boolean(
                id = "useStyledComponents",
                idToLog = "js.useStyledComponents"
            ),
            SettingIdWithPossibleValues.Boolean(
                id = "useReactRouterDom",
                idToLog = "js.useReactRouterDom"
            ),
            SettingIdWithPossibleValues.Boolean(
                id = "useReactRedux",
                idToLog = "js.useReactRedux"
            ),
        )


        val settingIdField = EventFields.String("settingId", settings.allowedIds)
        val settingValueField = EventFields.String("settingValue", settings.possibleValues)

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

        private val settingValueChangedEvent = GROUP.registerVarargEvent(
            "setting_value_changed",
            settingIdField,
            settingValueField,
            EventFields.PluginInfo,
        )

        // Log functions
        fun logDataOnProjectGenerated(project: Project?, projectCreationStats: ProjectCreationStats) {
            projectCreatedEvent.log(
                project,
                *projectCreationStats.toPairs().toTypedArray(),
                pluginInfoField
            )

        }

        fun logDataOnSettingValueChanged(
            settingId: String,
            settingValue: String
        ) {
            val idToLog = settings.getIdToLog(settingId) ?: return
            settingValueChangedEvent.log(
                settingIdField with idToLog,
                settingValueField with settingValue,
                pluginInfoField,
            )
        }


        fun logDataOnProjectGenerated(
            project: Project?,
            projectCreationStats: ProjectCreationStats,
            uiEditorUsageStats: UiEditorUsageStats
        ) {
            projectCreatedEvent.log(
                project,
                *projectCreationStats.toPairs().toTypedArray(),
                *uiEditorUsageStats.toPairs().toTypedArray(),
                pluginInfoField
            )
        }

        fun logUsedModuleTemplatesOnNewWizardProjectCreated(project: Project?, projectTemplateId: String, moduleTemplates: List<String>) {
            moduleTemplates.forEach { moduleTemplateId ->
                logModuleTemplateCreation(project, projectTemplateId, moduleTemplateId)
            }
        }

        fun logWizardOpenByHyperlink(project: Project?, templateId: String?) {
            projectOpenedByHyperlinkEvent.log(
                project,
                projectTemplateField.with(templateId ?: "none"),
                pluginInfoField
            )
        }

        private fun logModuleTemplateCreation(project: Project?, projectTemplateId: String, moduleTemplateId: String) {
            moduleTemplateCreatedEvent.log(
                project,
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


private sealed class SettingIdWithPossibleValues {
    abstract val id: String
    abstract val idToLog: String
    abstract val values: List<String>

    data class Enum(
        override val id: String,
        override val idToLog: String = id,
        override val values: List<String>
    ) : SettingIdWithPossibleValues()

    data class Boolean(
        override val id: String,
        override val idToLog: String = id,
    ) : SettingIdWithPossibleValues() {
        override val values: List<String> get() = listOf(true.toString(), false.toString())
    }
}

private class Settings(settingIdWithPossibleValues: List<SettingIdWithPossibleValues>) {
    constructor(vararg settingIdWithPossibleValues: SettingIdWithPossibleValues) : this(settingIdWithPossibleValues.toList())

    val allowedIds = settingIdWithPossibleValues.map { it.idToLog }
    val possibleValues = settingIdWithPossibleValues.flatMap { it.values }.distinct()
    private val id2IdToLog = settingIdWithPossibleValues.associate { it.id to it.idToLog }

    fun getIdToLog(id: String): String? = id2IdToLog.get(id)
}