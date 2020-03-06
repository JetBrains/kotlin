/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyType
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinArbitraryDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleConfigurationData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind

interface ModuleConfiguratorWithTests : ModuleConfiguratorWithSettings {
    companion object : ModuleConfiguratorSettings() {
        val testFramework by enumSetting<KotlinTestFramework>(
            "Test Framework",
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
        readingContext: ReadingContext,
        configurationData: ModuleConfigurationData,
        module: Module
    ): List<BuildSystemIR> =
        withSettingsOf(module) {
            with(readingContext) {
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
    JUNIT4("JUnit 4 Test Framework", ModuleType.jvm, listOf("test-junit")),
    JUNIT5("JUnit 5 Test Framework", ModuleType.jvm, listOf("test-junit5")),
    TEST_NG("Test NG Test Framework", ModuleType.jvm, listOf("test-testng")),
    JS("JavaScript Test Framework", ModuleType.js, listOf("test-js")),
    COMMON("Common Test Framework", ModuleType.common, listOf("test-common", "test-annotations-common")),
}