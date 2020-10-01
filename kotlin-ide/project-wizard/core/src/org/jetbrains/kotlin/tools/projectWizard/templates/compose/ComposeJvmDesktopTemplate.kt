/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates.compose

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleImportIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.addWithJavaIntoJvmTarget
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptorWithPath
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import org.jetbrains.kotlin.tools.projectWizard.templates.asSrcOf

class ComposeJvmDesktopTemplate : Template() {
    @NonNls
    override val id: String = "composeDesktopTemplate"

    override val title: String = KotlinNewProjectWizardBundle.message("module.template.compose.desktop.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.compose.desktop.description")

    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.jvm)

    override fun Writer.getIrsToAddToBuildFile(
        module: ModuleIR
    ) = irsList {
        +RepositoryIR(Repositories.JETBRAINS_COMPOSE_DEV)
        +RepositoryIR(DefaultRepository.JCENTER)
        +GradleOnlyPluginByNameIR("org.jetbrains.compose", version = Versions.JETBRAINS_COMPOSE)
        +runTaskIrs("MainKt")

        +GradleImportIR("org.jetbrains.compose.compose")
    }

    override fun Writer.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = listOf(
        CustomGradleDependencyDependencyIR("compose.desktop.all", dependencyType = DependencyType.MAIN)
    )

    override fun Writer.runArbitratyTask(module: ModuleIR): TaskResult<Unit> =
        BuildSystemPlugin.pluginRepositoreis.addValues(Repositories.JETBRAINS_COMPOSE_DEV)

    override fun updateTargetIr(module: ModuleIR, targetConfigurationIR: TargetConfigurationIR): TargetConfigurationIR =
        targetConfigurationIR.addWithJavaIntoJvmTarget()

    override fun Reader.getFileTemplates(module: ModuleIR) =
        buildList<FileTemplateDescriptorWithPath> {
            +(FileTemplateDescriptor("$id/main.kt", "main.kt".asPath()) asSrcOf SourcesetType.main)
        }
}
