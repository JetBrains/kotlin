/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSettingReference
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinBuildSystemPluginIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIRListBuilder
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.BrowserJsSinglePlatformModuleConfigurator.settingsValue
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JSConfigurator.Companion.isApplication
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsBrowserBasedConfigurator.Companion.browserSubTarget
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsBrowserBasedConfigurator.Companion.cssSupport
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsNodeBasedConfigurator.Companion.nodejsSubTarget
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GradlePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.templates.ReactJsClientTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.SimpleJsClientTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.SimpleNodeJsTemplate
import java.nio.file.Path

interface JSConfigurator : ModuleConfiguratorWithModuleType, ModuleConfiguratorWithSettings {
    override val moduleType: ModuleType get() = ModuleType.js

    override fun Writer.runArbitraryTask(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> =
        GradlePlugin.gradleProperties
            .addValues("kotlin.js.generate.executable.default" to "false")

    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> =
        super.getConfiguratorSettings() + kind

    companion object : ModuleConfiguratorSettings() {
        val kind by enumSetting<JsTargetKind>(
            KotlinNewProjectWizardBundle.message("module.configurator.js.target.settings.kind"),
            GenerationPhase.PROJECT_GENERATION
        ) {
            defaultValue = value(JsTargetKind.APPLICATION)
            filter = filter@{ reference, kindCandidate ->
                when {
                    reference !is ModuleConfiguratorSettingReference<*, *> -> false
                    kindCandidate == JsTargetKind.LIBRARY
                            && (reference.module?.template is SimpleJsClientTemplate ||
                            reference.module?.template is ReactJsClientTemplate ||
                            reference.module?.template is SimpleNodeJsTemplate) -> false
                    else -> true
                }
            }
        }

        fun Reader.isApplication(module: Module): Boolean =
            settingsValue(module, kind) == JsTargetKind.APPLICATION
    }
}

interface JsBrowserBasedConfigurator {
    companion object : ModuleConfiguratorSettings() {
        val cssSupport by JSConfigurator.booleanSetting(
            KotlinNewProjectWizardBundle.message("module.configurator.js.css"),
            GenerationPhase.PROJECT_GENERATION
        ) {
            defaultValue = value(true)
        }

        private fun Reader.hasCssSupport(module: Module): Boolean =
            settingsValue(module, cssSupport)

        fun GradleIRListBuilder.browserSubTarget(module: Module, reader: Reader) {
            "browser" {
                if (reader.isApplication(module)) {
                    applicationSupport()
                    if (reader.hasCssSupport(module)) {
                        applicationCssSupport()
                    }
                }
                if (reader.settingValue(module, ModuleConfiguratorWithTests.testFramework) != KotlinTestFramework.NONE
                ) {
                    testTask(cssSupport = reader.hasCssSupport(module))
                }
            }
        }
    }
}

interface JsNodeBasedConfigurator {
    companion object : ModuleConfiguratorSettings() {
        fun GradleIRListBuilder.nodejsSubTarget(module: Module, reader: Reader) {
            "nodejs" {
                if (reader.isApplication(module)) {
                    applicationSupport()
                }
            }
        }
    }
}

abstract class JsSinglePlatformModuleConfigurator :
    JSConfigurator,
    ModuleConfiguratorWithTests,
    SinglePlatformModuleConfigurator,
    ModuleConfiguratorWithSettings {
    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> =
        super<ModuleConfiguratorWithTests>.getConfiguratorSettings() +
                super<JSConfigurator>.getConfiguratorSettings()

    @NonNls
    override val suggestedModuleName = "js"

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JS

    override val canContainSubModules = false

    override fun createKotlinPluginIR(configurationData: ModulesToIrConversionData, module: Module): KotlinBuildSystemPluginIR? =
        KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.js,
            version = configurationData.kotlinVersion
        )

    override fun createBuildFileIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ): List<BuildSystemIR> = irsList {
        "kotlin" {
            "js" {
                subTarget(module, reader)
            }
        }
    }

    protected abstract fun GradleIRListBuilder.subTarget(module: Module, reader: Reader)
}

object BrowserJsSinglePlatformModuleConfigurator : JsSinglePlatformModuleConfigurator(), JsBrowserBasedConfigurator {
    @NonNls
    override val id = "jsBrowserSinglePlatform"

    override val moduleKind = ModuleKind.singleplatformJsBrowser

    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> {
        return super.getConfiguratorSettings() +
                cssSupport
    }

    override fun GradleIRListBuilder.subTarget(module: Module, reader: Reader) {
        browserSubTarget(module, reader)
    }

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.simple.js.browser")
}

object NodeJsSinglePlatformModuleConfigurator : JsSinglePlatformModuleConfigurator() {
    @NonNls
    override val id = "jsNodeSinglePlatform"

    override val moduleKind = ModuleKind.singleplatformJsNode

    override fun GradleIRListBuilder.subTarget(module: Module, reader: Reader) {
        nodejsSubTarget(module, reader)
    }

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.simple.js.node")
}

fun GradleIRListBuilder.applicationSupport() {
    +"binaries.executable()"
}

fun GradleIRListBuilder.applicationCssSupport() {
    "webpackTask" {
        +"cssSupport.enabled = true"
    }
    "runTask" {
        +"cssSupport.enabled = true"
    }
}

fun GradleIRListBuilder.testTask(cssSupport: Boolean) {
    "testTask" {
        "useKarma" {
            +"useChromeHeadless()"
            if (cssSupport) {
                +"webpackConfig.cssSupport.enabled = true"
            }
        }
    }
}