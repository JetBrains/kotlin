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
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.inContextOfModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.moduleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.pomIR
import org.jetbrains.kotlin.tools.projectWizard.settings.JavaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.templates.*

class ComposeJvmDesktopTemplate : Template() {
    @NonNls
    override val id: String = "composeDesktopTemplate"

    override val title: String = KotlinNewProjectWizardBundle.message("module.template.compose.desktop.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.compose.desktop.description")

    override fun isSupportedByModuleType(module: Module): Boolean =
        module.configurator.moduleType == ModuleType.jvm

    override fun isApplicableTo(reader: Reader, module: Module): Boolean =
        module.kind == ModuleKind.singleplatformJvm

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
        CustomGradleDependencyDependencyIR("compose.desktop.all", dependencyType = DependencyType.MAIN, DependencyKind.implementation)
    )

    override fun Writer.runArbitratyTask(module: ModuleIR): TaskResult<Unit> =
        BuildSystemPlugin.pluginRepositoreis.addValues(Repositories.JETBRAINS_COMPOSE_DEV)

    override fun Reader.getFileTemplates(module: ModuleIR) = buildList<FileTemplateDescriptorWithPath> {
        val dependsOnMppModule: Module? =
            module.originalModule.dependencies.map { moduleByReference(it) }.firstOrNull { it.template is ComposeMppModuleTemplate }
        if (dependsOnMppModule == null) {
            +(FileTemplateDescriptor("$id/main.kt", "main.kt".asPath()) asSrcOf SourcesetType.main)
        } else {
            val javaPackage = dependsOnMppModule.javaPackage(pomIR()).asCodePackage()
            +(FileTemplateDescriptor("composeMpp/main.kt.vm", "main.kt".asPath())
                    asSrcOf SourcesetType.main
                    withSettings ("sharedPackage" to javaPackage)
                    )
        }
    }
}
