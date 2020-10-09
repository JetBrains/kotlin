/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.AndroidConfigIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.BuildScriptDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.BuildScriptRepositoryIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidTargetConfigurator.createAndroidPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GradlePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import java.nio.file.Path

object AndroidSinglePlatformModuleConfigurator :
    SinglePlatformModuleConfigurator,
    AndroidModuleConfigurator {
    override val moduleKind: ModuleKind get() = ModuleKind.singleplatformAndroid

    override fun getNewAndroidManifestPath(module: Module): Path? = null

    @NonNls
    override val id = "android"

    @NonNls
    override val suggestedModuleName = "android"
    override val text = KotlinNewProjectWizardBundle.message("module.configurator.android")

    override val requiresRootBuildFile: Boolean = true

    override val resourcesDirectoryName: String = "res"
    override val kotlinDirectoryName: String = "java"


    override fun createRootBuildFileIrs(configurationData: ModulesToIrConversionData): List<BuildSystemIR> = irsList {
        listOf(
            DefaultRepository.GRADLE_PLUGIN_PORTAL,
            DefaultRepository.JCENTER,
            DefaultRepository.GOOGLE,
            configurationData.kotlinVersion.repository
        ).forEach { repository ->
            +BuildScriptRepositoryIR(RepositoryIR((repository)))
        }

        irsList {
            "classpath"(const("org.jetbrains.kotlin:kotlin-gradle-plugin:${configurationData.kotlinVersion.version}"))
            "classpath"(const("com.android.tools.build:gradle:${Versions.GRADLE_PLUGINS.ANDROID}"))
        }.forEach {
            +BuildScriptDependencyIR(it)
        }
    }

    override fun createBuildFileIRs(reader: Reader, configurationData: ModulesToIrConversionData, module: Module) = irsList {
        +super<AndroidModuleConfigurator>.createBuildFileIRs(reader, configurationData, module)
        +AndroidConfigIR(
            javaPackage = when (reader.createAndroidPlugin(module)) {
                AndroidGradlePlugin.APPLICATION -> module.javaPackage(configurationData.pomIr)
                AndroidGradlePlugin.LIBRARY -> null
            },
            newManifestPath = getNewAndroidManifestPath(module),
            printVersionCode = true,
            printBuildTypes = true,
        )
    }

    override fun createKotlinPluginIR(configurationData: ModulesToIrConversionData, module: Module): KotlinBuildSystemPluginIR? =
        KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.android,
            version = configurationData.kotlinVersion,
            priority = 2
        )

    override fun createModuleIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ) = buildList<BuildSystemIR> {
        +super<AndroidModuleConfigurator>.createModuleIRs(reader, configurationData, module)

        +ArtifactBasedLibraryDependencyIR(
            MavenArtifact(DefaultRepository.GOOGLE, "androidx.appcompat", "appcompat"),
            version = Versions.ANDROID.ANDROIDX_APPCOMPAT,
            dependencyType = DependencyType.MAIN
        )

        +ArtifactBasedLibraryDependencyIR(
            MavenArtifact(DefaultRepository.GOOGLE, "androidx.constraintlayout", "constraintlayout"),
            version = Versions.ANDROID.ANDROIDX_CONSTRAINTLAYOUT,
            dependencyType = DependencyType.MAIN
        )
    }

    override fun Writer.runArbitraryTask(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = computeM {
        val sharedModule = module.dependencies
            .map { if (it is ModuleReference.ByModule) it.module else null }
            .firstOrNull { it?.configurator == MppModuleConfigurator }

        val javaPackage = module.javaPackage(configurationData.pomIr)
        val sharedPackage = sharedModule?.javaPackage(configurationData.pomIr)

        val settings = mapOf(
            "package" to javaPackage.asCodePackage(),
            "sharedPackage" to sharedPackage?.asCodePackage()
        )
        TemplatesPlugin.addFileTemplates.execute(
            listOf(
                FileTemplate(AndroidModuleConfigurator.FileTemplateDescriptors.activityMainXml, modulePath, settings),
                FileTemplate(getAndroidManifestXml(module), modulePath, settings),
                FileTemplate(AndroidModuleConfigurator.FileTemplateDescriptors.colorsXml, modulePath, settings),
                FileTemplate(AndroidModuleConfigurator.FileTemplateDescriptors.stylesXml, modulePath, settings),
                FileTemplate(AndroidModuleConfigurator.FileTemplateDescriptors.mainActivityKt(javaPackage), modulePath, settings)
            )
        )
        GradlePlugin.gradleProperties.addValues("android.useAndroidX" to true)
    }

    override fun Reader.createAndroidPlugin(module: Module): AndroidGradlePlugin =
        AndroidGradlePlugin.APPLICATION
}