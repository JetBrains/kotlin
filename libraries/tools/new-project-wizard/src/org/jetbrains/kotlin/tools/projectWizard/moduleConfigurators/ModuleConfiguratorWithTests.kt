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
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyType
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinArbitraryDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind

interface ModuleConfiguratorWithTests : ModuleConfiguratorWithSettings {
    companion object : ModuleConfiguratorSettings() {
        val testFramework by enumSetting<KotlinTestFramework>(
            KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework"),
            neededAtPhase = GenerationPhase.PROJECT_GENERATION
        ) {
            filter = filter@{ reference, kotlinTestFramework ->
                if (reference !is ModuleConfiguratorSettingReference<*, *>) return@filter true

                val moduleType = reference.module?.configurator?.safeAs<ModuleConfiguratorWithModuleType>()?.moduleType
                kotlinTestFramework.moduleType == moduleType
            }
        }
    }

    fun defaultTestFramework(): KotlinTestFramework

    override fun createModuleIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ): List<BuildSystemIR> =
        withSettingsOf(module) {
            reader {
                testFramework.reference.settingValue.dependencyNames.map { dependencyName ->
                    KotlinArbitraryDependencyIR(
                        dependencyName,
                        isInMppModule = module.kind
                            .let { it == ModuleKind.multiplatform || it == ModuleKind.target },
                        version = KotlinPlugin::version.propertyValue,
                        dependencyType = DependencyType.TEST
                    )
                }
            }
        }


    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> = listOf(testFramework)
}


enum class KotlinTestFramework(
    override val text: String,
    val moduleType: ModuleType,
    val dependencyNames: List<String>
) : DisplayableSettingItem {
    JUNIT4(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.junit4"),
        ModuleType.jvm,
        listOf("test-junit")
    ),
    JUNIT5(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.junit5"),
        ModuleType.jvm,
        listOf("test-junit5")
    ),
    TEST_NG(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.test.ng"),
        ModuleType.jvm,
        listOf("test-testng")
    ),
    JS(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.js"),
        ModuleType.js,
        listOf("test-js")
    ),
    COMMON(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.common"),
        ModuleType.common,
        listOf("test-common", "test-annotations-common")
    )
}