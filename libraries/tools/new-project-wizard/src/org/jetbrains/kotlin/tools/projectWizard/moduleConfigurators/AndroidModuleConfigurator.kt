/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardKotlinVersion
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.AndroidConfigIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.AndroidPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.JavaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import java.nio.file.Path

interface AndroidModuleConfigurator : ModuleConfigurator,
    ModuleConfiguratorWithSettings,
    ModuleConfiguratorWithModuleType,
    GradleModuleConfigurator {

    override val moduleType: ModuleType
        get() = ModuleType.android

    override fun getPluginSettings(): List<PluginSettingReference<Any, SettingType<Any>>> =
        listOf(AndroidPlugin::androidSdkPath.reference)

    override fun createBuildFileIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ) = buildList<BuildSystemIR> {
        +GradleOnlyPluginByNameIR(reader.createAndroidPlugin(module).pluginName, priority = 1)

        +GradleOnlyPluginByNameIR("kotlin-android-extensions", priority = 3)
        +AndroidConfigIR(
            when (reader.createAndroidPlugin(module)) {
                AndroidGradlePlugin.APPLICATION -> module.javaPackage(configurationData.pomIr)
                AndroidGradlePlugin.LIBRARY -> null
            }
        )
        +createRepositories(configurationData.kotlinVersion).map(::RepositoryIR)
    }

    fun Reader.createAndroidPlugin(module: Module): AndroidGradlePlugin

    override fun Reader.createSettingsGradleIRs(module: Module) = buildList<BuildSystemIR> {
        +createRepositories(KotlinPlugin::version.propertyValue).map { PluginManagementRepositoryIR(RepositoryIR(it)) }
        +AndroidResolutionStrategyIR(Versions.GRADLE_PLUGINS.ANDROID)
    }

    override fun createModuleIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ): List<BuildSystemIR> =
        buildList {
            +ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.GOOGLE, "androidx.core", "core-ktx"),
                version = Versions.ANDROID.ANDROIDX_CORE_KTX,
                dependencyType = DependencyType.MAIN
            )
        }


    override fun createStdlibType(configurationData: ModulesToIrConversionData, module: Module): StdlibType? =
        StdlibType.StdlibJdk7

    object FileTemplateDescriptors {
        val activityMainXml = FileTemplateDescriptor(
            "android/activity_main.xml.vm",
            "src" / "main" / "res" / "layout" / "activity_main.xml"
        )

        val androidManifestXml = FileTemplateDescriptor(
            "android/AndroidManifest.xml.vm",
            "src" / "main" / "AndroidManifest.xml"
        )

        val androidanifestForLibraryXml = FileTemplateDescriptor(
            "android/AndroidManifestLibrary.xml.vm",
            "src" / "main" / "AndroidManifest.xml"
        )

        fun mainActivityKt(javaPackage: JavaPackage) = FileTemplateDescriptor(
            "android/MainActivity.kt.vm",
            "src" / "main" / "java" / javaPackage.asPath() / "MainActivity.kt"
        )
    }

    companion object {
        fun createRepositories(kotlinVersion: WizardKotlinVersion) = buildList<Repository> {
            +DefaultRepository.GRADLE_PLUGIN_PORTAL
            +DefaultRepository.GOOGLE
            +DefaultRepository.JCENTER
            +kotlinVersion.repository
        }
    }
}

object AndroidTargetConfigurator : TargetConfigurator,
    SimpleTargetConfigurator,
    AndroidModuleConfigurator,
    SingleCoexistenceTargetConfigurator,
    ModuleConfiguratorSettings() {
    override val moduleSubType = ModuleSubType.android
    override val moduleType = ModuleType.android

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.android")

    override fun Reader.createAndroidPlugin(module: Module): AndroidGradlePlugin =
        withSettingsOf(module) { androidPlugin.reference.settingValue }

    override fun getConfiguratorSettings() = buildList<ModuleConfiguratorSetting<*, *>> {
        +super.getConfiguratorSettings()
        +androidPlugin
    }

    override fun Writer.runArbitraryTask(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = computeM {
        val javaPackage = module.javaPackage(configurationData.pomIr)
        val settings = mapOf("package" to javaPackage.asCodePackage())
        TemplatesPlugin::addFileTemplates.execute(
            listOf(
                FileTemplate(AndroidModuleConfigurator.FileTemplateDescriptors.androidanifestForLibraryXml, modulePath, settings)
            )
        )
    }

    val androidPlugin by enumSetting<AndroidGradlePlugin>(
        KotlinNewProjectWizardBundle.message("module.configurator.android.setting.android.plugin"),
        neededAtPhase = GenerationPhase.PROJECT_GENERATION
    )
}

enum class AndroidGradlePlugin(override val text: String, @NonNls val pluginName: String) : DisplayableSettingItem {
    APPLICATION(
        KotlinNewProjectWizardBundle.message("module.configurator.android.setting.android.plugin.application"),
        "com.android.application"
    ),
    LIBRARY(
        KotlinNewProjectWizardBundle.message("module.configurator.android.setting.android.plugin.library"),
        "com.android.library"
    )
}