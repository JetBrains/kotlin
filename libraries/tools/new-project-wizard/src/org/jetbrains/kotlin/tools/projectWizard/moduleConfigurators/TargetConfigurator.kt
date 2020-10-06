/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleStringConstIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetAccessIR
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JSConfigurator.Companion.jsCompilerParam
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsBrowserBasedConfigurator.Companion.browserSubTarget
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsBrowserBasedConfigurator.Companion.cssSupport
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsNodeBasedConfigurator.Companion.nodejsSubTarget
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsNodeTargetConfigurator.createTargetIrs
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.buildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind


interface TargetConfigurator : ModuleConfiguratorWithModuleType {
    override val moduleKind get() = ModuleKind.target

    fun canCoexistsWith(other: List<TargetConfigurator>): Boolean = true

    fun Reader.createTargetIrs(module: Module): List<BuildSystemIR>
    fun createInnerTargetIrs(
        reader: Reader,
        module: Module
    ): List<BuildSystemIR> = emptyList()
}

abstract class TargetConfiguratorWithTests : ModuleConfiguratorWithTests, TargetConfigurator

interface SingleCoexistenceTargetConfigurator : TargetConfigurator {
    override fun canCoexistsWith(other: List<TargetConfigurator>): Boolean =
        other.none { it == this }
}

interface SimpleTargetConfigurator : TargetConfigurator {
    val moduleSubType: ModuleSubType
    override val moduleType get() = moduleSubType.moduleType
    override val id get() = "${moduleSubType.name}Target"

    override val suggestedModuleName: String? get() = moduleSubType.name


    override fun Reader.createTargetIrs(
        module: Module
    ): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(moduleSubType),
            createInnerTargetIrs(this@createTargetIrs, module).toPersistentList()
        )
    }
}

internal fun Module.createTargetAccessIr(
    moduleSubType: ModuleSubType,
    additionalParams: List<Any?> = listOf()
) =
    TargetAccessIR(
        moduleSubType,
        name.takeIf { it != moduleSubType.name },
        additionalParams.filterNotNull()
    )


interface JsTargetConfigurator : JSConfigurator, TargetConfigurator, SingleCoexistenceTargetConfigurator, ModuleConfiguratorWithSettings

enum class JsTargetKind(override val text: String) : DisplayableSettingItem {
    LIBRARY(KotlinNewProjectWizardBundle.message("module.configurator.js.target.settings.kind.library")),
    APPLICATION(KotlinNewProjectWizardBundle.message("module.configurator.js.target.settings.kind.application"))
}

enum class JsCompiler(override val text: String) : DisplayableSettingItem {
    IR("IR"),
    LEGACY("LEGACY"),
    BOTH("BOTH")
}

object JsBrowserTargetConfigurator : JsTargetConfigurator, ModuleConfiguratorWithTests {
    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> =
        super<ModuleConfiguratorWithTests>.getConfiguratorSettings() +
                super<JsTargetConfigurator>.getConfiguratorSettings() +
                cssSupport

    @NonNls
    override val id = "jsBrowser"

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.js.browser")

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JS

    override fun Reader.createTargetIrs(
        module: Module
    ): List<BuildSystemIR> = irsList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(
                ModuleSubType.js,
                paramsWithJsCompiler(module)
            )
        ) {
            browserSubTarget(module, this@createTargetIrs)
        }
    }
}

object JsNodeTargetConfigurator : JsTargetConfigurator {
    @NonNls
    override val id = "jsNode"

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.js.node")

    override fun Reader.createTargetIrs(
        module: Module
    ): List<BuildSystemIR> = irsList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(
                ModuleSubType.js,
                paramsWithJsCompiler(module)
            )
        ) {
            nodejsSubTarget(module, this@createTargetIrs)
        }
    }
}

internal fun Reader.paramsWithJsCompiler(module: Module): List<String> = jsCompilerParam(module)?.let {
    listOf(it)
} ?: emptyList()

object CommonTargetConfigurator : TargetConfiguratorWithTests(), SimpleTargetConfigurator, SingleCoexistenceTargetConfigurator {
    override val moduleSubType = ModuleSubType.common
    override val text: String = KotlinNewProjectWizardBundle.message("module.configurator.common")

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.COMMON
}

object JvmTargetConfigurator : JvmModuleConfigurator,
    TargetConfigurator,
    SimpleTargetConfigurator {
    override val moduleSubType = ModuleSubType.jvm

    override val text: String = KotlinNewProjectWizardBundle.message("module.configurator.jvm")

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JUNIT4

    override fun createInnerTargetIrs(
        reader: Reader,
        module: Module
    ): List<BuildSystemIR> = irsList {
        +super<SimpleTargetConfigurator>.createInnerTargetIrs(reader, module)
        reader {
            inContextOfModuleConfigurator(module) {
                val targetVersionValue = JvmModuleConfigurator.targetJvmVersion.reference.settingValue.value
                if (buildSystemType.isGradle) {
                    "compilations.all" {
                        "kotlinOptions.jvmTarget" assign GradleStringConstIR(targetVersionValue)
                    }

                }
                if (Settings.javaSupport.reference.settingValue) {
                    "withJava"()
                }
            }
            val testFramework = inContextOfModuleConfigurator(module) { ModuleConfiguratorWithTests.testFramework.reference.settingValue }
            if (testFramework != KotlinTestFramework.NONE) {
                testFramework.usePlatform?.let { usePlatform ->
                    "testRuns[\"test\"].executionTask.configure" {
                        +"$usePlatform()"
                    }
                }
            }
        }
    }

    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> =
        super.getConfiguratorSettings() +
                Settings.javaSupport

    object Settings : ModuleConfiguratorSettings() {
        val javaSupport by booleanSetting(
            KotlinNewProjectWizardBundle.message("module.configurator.jvm.setting.java.support"),
            GenerationPhase.PROJECT_GENERATION
        ) {
            description = KotlinNewProjectWizardBundle.message("module.configurator.jvm.setting.java.support.description")
            defaultValue = value(false)
        }
    }
}
