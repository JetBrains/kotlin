/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.compute
import org.jetbrains.kotlin.tools.projectWizard.core.computeM
import org.jetbrains.kotlin.tools.projectWizard.core.context.SettingsWritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.Plugins
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.applyProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.Wizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.YamlWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.parseYaml
import java.nio.file.Path

class ProjectTemplateBasedTestWizard(
    private val projectTemplate: ProjectTemplate,
    private val buildSystem: BuildSystem,
    private val projectDirectory: Path,
    servicesManager: ServicesManager,
    private val additionalYamlSettings: String?
) : Wizard(
    Plugins.allPlugins,
    servicesManager,
    isUnitTestMode = true
) {
    private val settingsWritingContext = SettingsWritingContext(context, servicesManager, isUnitTestMode = true)

    override fun apply(
        services: List<WizardService>,
        phases: Set<GenerationPhase>,
        onTaskExecuting: (PipelineTask) -> Unit
    ): TaskResult<Unit> =
        computeM {
            super.apply(services, setOf(GenerationPhase.PREPARE), onTaskExecuting).ensure()
            with(settingsWritingContext) {
                applyProjectTemplate(projectTemplate)
                BuildSystemPlugin::type.reference.setValue(buildSystem.buildSystemType)
                StructurePlugin::projectPath.reference.setValue(projectDirectory)
                StructurePlugin::name.reference.setValue(projectTemplate.id)
                StructurePlugin::groupId.reference.setValue(GROUP_ID)
                StructurePlugin::artifactId.reference.setValue(ARTIFACT_ID)
                applyAdditionalSettingsFromYaml().ensure()
            }

            super.apply(services, phases, onTaskExecuting)
        }

    private fun SettingsWritingContext.applyAdditionalSettingsFromYaml() = compute {
        if (additionalYamlSettings != null) {
            val (additionalSettings) = parseYaml(additionalYamlSettings, pluginSettings)
            additionalSettings.forEach { (reference, value) -> reference.setValue(value) }
        }
    }

    companion object {
        private const val GROUP_ID = "me.user"
        private const val ARTIFACT_ID = "artifactId"

        fun createByDirectory(
            directory: Path,
            buildSystem: BuildSystem,
            projectDirectory: Path,
            servicesManager: ServicesManager
        ): ProjectTemplateBasedTestWizard {
            val projectTemplateId = directory.fileName.toString()
            val projectTemplate = ProjectTemplate.byId(projectTemplateId)
                ?: error("Directory name should be a valid template id but was $projectTemplateId")
            val additionalYamlSettings = readSettingsYamlWithoutDefaultStructure(directory)
            return ProjectTemplateBasedTestWizard(projectTemplate, buildSystem, projectDirectory, servicesManager, additionalYamlSettings)
        }
    }
}
