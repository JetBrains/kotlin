/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates.compose

import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleImportIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.addWithJavaIntoJvmTarget
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.inContextOfModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.moduleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.plugins.pomIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.JavaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptorWithPath
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import org.jetbrains.kotlin.tools.projectWizard.templates.asSrcOf

class ComposeAndroidTemplate : Template() {
    @NonNls
    override val id: String = "composeAndroid"

    override val title: String = KotlinNewProjectWizardBundle.message("module.template.compose.desktop.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.compose.desktop.description")


    override fun isSupportedByModuleType(module: Module, projectKind: ProjectKind): Boolean =
        module.configurator.moduleType == ModuleType.android && projectKind == ProjectKind.COMPOSE

    override fun isApplicableTo(reader: Reader, module: Module): Boolean =
        module.kind == ModuleKind.singleplatformAndroid

    override fun Writer.getIrsToAddToBuildFile(
        module: ModuleIR
    ) = irsList {
        +GradleOnlyPluginByNameIR("org.jetbrains.compose", version = Versions.JETBRAINS_COMPOSE)
        +RepositoryIR(Repositories.JETBRAINS_COMPOSE_DEV)
        +RepositoryIR(DefaultRepository.JCENTER)
        +RepositoryIR(DefaultRepository.GOOGLE)
    }

    override fun Reader.updateBuildFileIRs(irs: List<BuildSystemIR>): List<BuildSystemIR> = irs.filterNot {
        it.safeAs<GradleOnlyPluginByNameIR>()?.pluginId == AndroidModuleConfigurator.DEPENDENCIES.KOTLIN_ANDROID_EXTENSIONS_NAME
    }


    override fun Reader.updateModuleIR(module: ModuleIR): ModuleIR {
        val irs = module.irs.filterNot { ir ->
            ir == AndroidSinglePlatformModuleConfigurator.DEPENDENCIES.APP_COMPAT
                    || ir == AndroidSinglePlatformModuleConfigurator.DEPENDENCIES.CONSTRAINT_LAYOUT
                    || ir == AndroidModuleConfigurator.DEPENDENCIES.MATERIAL
        }
        return module.withReplacedIrs(irs.toPersistentList())
    }

    override fun Writer.runArbitratyTask(module: ModuleIR): TaskResult<Unit> = compute {
        BuildSystemPlugin.pluginRepositoreis.addValues(Repositories.JETBRAINS_COMPOSE_DEV).ensure()

        //TODO hacky!
        TemplatesPlugin.fileTemplatesToRender.update { templates ->
            templates.mapNotNull { template ->
                val descriptor = template.descriptor as? FileTemplateDescriptor
                when {
                    descriptor == AndroidModuleConfigurator.FileTemplateDescriptors.activityMainXml
                            || descriptor == AndroidModuleConfigurator.FileTemplateDescriptors.colorsXml
                            || descriptor == AndroidModuleConfigurator.FileTemplateDescriptors.stylesXml -> null
                    descriptor?.templateId == "android/MainActivity.kt.vm" -> {
                        template.copy(descriptor = mainActivityKt(module.originalModule.javaPackage(pomIR())))
                    }
                    descriptor?.templateId == "android/AndroidManifest.xml.vm" -> {
                        template.copy(descriptor = Descriptors.manifestXml)
                    }
                    else -> template
                }
            }.asSuccess()
        }
    }

    private fun mainActivityKt(javaPackage: JavaPackage) = FileTemplateDescriptor(
        "composeAndroid/MainActivity.kt.vm",
        "src" / "main" / "java" / javaPackage.asPath() / "MainActivity.kt"
    )

    object Descriptors {
        val manifestXml = FileTemplateDescriptor(
            templateId = "composeAndroid/AndroidManifest.xml.vm",
            relativePath = "src" / "main" / "AndroidManifest.xml",
        )
    }
}
