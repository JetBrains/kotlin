/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.projectWizard

import com.intellij.internal.statistic.eventLog.EventFields
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.EventPair
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfoById
import org.jetbrains.kotlin.idea.KotlinPluginUtil

interface WizardStats {
    fun toPairs(): ArrayList<EventPair<*>>
}

class WizardStatsService : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    companion object {
        private val GROUP = EventLogGroup("kotlin.ide.new.project", 1)

        private const val eventProjectCreated = "project_created"
        private const val eventProjectOpenedByHyperlink = "wizard_opened_by_hyperlink"

        private const val contextGroup = "group"
        private const val contextProjectTemplate = "project_template"
        private const val contextBuildSystem = "build_system"
        private const val contextModulesCreated = "modules_created"
        private const val contextModulesRemoved = "modules_removed"
        private const val contextModuleTemplateChanged = "module_template_changed"

        val groupField = EventFields.String(contextGroup).withCustomEnum("kotlin_wizard_groups")
        val projectTemplateField = EventFields.String(contextProjectTemplate).withCustomEnum("kotlin_wizard_templates")
        val buildSystemField = EventFields.String(contextBuildSystem).withCustomEnum("kotlin_wizard_build_systems")

        val modulesCreatedField = EventFields.Int(contextModulesCreated)
        val modulesRemovedField = EventFields.Int(contextModulesRemoved)
        val moduleTemplateChangedField = EventFields.Int(contextModuleTemplateChanged)

        private val pluginInfo = EventFields.PluginInfo.with(getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID))

        val projectCreatedEvent = GROUP.registerVarargEvent(
            eventProjectCreated,
            groupField,
            projectTemplateField,
            buildSystemField,
            modulesCreatedField,
            modulesRemovedField,
            moduleTemplateChangedField,
            EventFields.PluginInfo
        )

        val projectOpenedByHyperlink = GROUP.registerVarargEvent(
            eventProjectOpenedByHyperlink,
            projectTemplateField,
            EventFields.PluginInfo
        )

        fun logDataOnProjectGenerated(projectCreationStats: ProjectCreationStats) {
            projectCreatedEvent.log(*projectCreationStats.toPairs().toTypedArray(),
                                    pluginInfo
            )
        }

        fun logDataOnProjectGenerated(projectCreationStats: ProjectCreationStats, uiEditorUsageStats: UiEditorUsageStats) {
            projectCreatedEvent.log(*projectCreationStats.toPairs().toTypedArray(),
                                    *uiEditorUsageStats.toPairs().toTypedArray(),
                                    pluginInfo
            )
        }

        fun logWizardOpenByHyperlink(templateId: String?) {
            projectOpenedByHyperlink.log(projectTemplateField.with(templateId ?: "none"),
                                         pluginInfo
            )
        }
    }

    data class ProjectCreationStats(
            val group: String,
            val projectTemplateId: String,
            val buildSystemType: String
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



