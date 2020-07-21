/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSettingReference
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinBuildSystemPluginIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIRListBuilder
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.templates.SimpleJsClientTemplate

interface JSConfigurator : ModuleConfiguratorWithModuleType, ModuleConfiguratorWithSettings {
    override val moduleType: ModuleType get() = ModuleType.js

    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> =
        super.getConfiguratorSettings() + kind

    fun Reader.isApplication(module: Module): Boolean =
        settingsValue(module, kind) == JsTargetKind.APPLICATION

    fun Reader.hasCssSupport(module: Module): Boolean =
        settingsValue(module, cssSupport)

    fun GradleIRListBuilder.browserSubTarget(module: Module, reader: Reader) {
        "browser" {
            if (reader.isApplication(module)) {
                applicationSupport()
            }
            if (reader.hasCssSupport(module)) {
                if (reader.isApplication(module)) {
                    applicationCssSupport()
                }
            }
            if (this@JSConfigurator is ModuleConfiguratorWithTests
                && reader.settingValue(module, ModuleConfiguratorWithTests.testFramework) != KotlinTestFramework.NONE
            ) {
                testTask(cssSupport = reader.hasCssSupport(module))
            }
        }
    }

    fun GradleIRListBuilder.nodejsSubTarget(module: Module, reader: Reader) {
        "nodejs" {
            if (reader.isApplication(module)) {
                applicationSupport()
            }
        }
    }

    companion object : ModuleConfiguratorSettings() {
        val kind by enumSetting<JsTargetKind>(
            KotlinNewProjectWizardBundle.message("module.configurator.js.target.settings.kind"),
            GenerationPhase.PROJECT_GENERATION
        ) {
            defaultValue = value(JsTargetKind.APPLICATION)
            filter = filter@{ reference, kindCandidate ->
                when {
                    reference !is ModuleConfiguratorSettingReference<*, *> -> false
                    kindCandidate == JsTargetKind.LIBRARY && reference.module?.template is SimpleJsClientTemplate -> false
                    else -> true
                }
            }
        }

        val cssSupport by booleanSetting(
            KotlinNewProjectWizardBundle.message("module.configurator.js.css"),
            GenerationPhase.PROJECT_GENERATION
        ) {
            defaultValue = value(true)
        }
    }
}

enum class JsTarget {
    BROWSER,
    NODE;
}

abstract class JsSinglePlatformModuleConfigurator(
    jsTarget: JsTarget
) : JSConfigurator, ModuleConfiguratorWithTests, SinglePlatformModuleConfigurator,
    ModuleConfiguratorWithSettings {
    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> =
        super<ModuleConfiguratorWithTests>.getConfiguratorSettings() +
                super<JSConfigurator>.getConfiguratorSettings()

    override val moduleKind = ModuleKind.singleplatformJs

    @NonNls
    override val suggestedModuleName = "js"

    @NonNls
    override val id = "js${jsTarget.name.toLowerCase().capitalize()}SinglePlatform"

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

object BrowserJsSinglePlatformModuleConfigurator : JsSinglePlatformModuleConfigurator(
    JsTarget.BROWSER
) {
    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> {
        return super.getConfiguratorSettings() +
                JSConfigurator.cssSupport
    }

    override fun GradleIRListBuilder.subTarget(module: Module, reader: Reader) {
        browserSubTarget(module, reader)
    }

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.simple.js.browser")
}

object NodeJsSinglePlatformModuleConfigurator : JsSinglePlatformModuleConfigurator(
    JsTarget.NODE
) {
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