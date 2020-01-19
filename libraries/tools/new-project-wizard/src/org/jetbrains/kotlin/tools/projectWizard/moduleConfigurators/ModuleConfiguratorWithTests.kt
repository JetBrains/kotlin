/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ModuleConfiguratorSettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.TemplateSettingReference
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

abstract class ModuleConfiguratorWithTests : ModuleConfiguratorWithSettings() {
    val framework by enumSetting<KotlinTestFramework>(
        "Test Framework",
        neededAtPhase = GenerationPhase.PROJECT_GENERATION
    ) {
        defaultValue = defaultTestFramework()

        filter = filter@{ reference, kotlinTestFramework ->
            if (reference !is ModuleConfiguratorSettingReference<*, *>) return@filter true

            val moduleType = reference.module?.configurator?.moduleType
            kotlinTestFramework.moduleType == moduleType
        }
    }

    abstract fun defaultTestFramework(): KotlinTestFramework

    override fun ValuesReadingContext.createModuleIRs(configurationData: ModuleConfigurationData, module: Module): List<BuildSystemIR> =
        withSettingsOf(module) {
            framework.reference.settingValue.dependencyNames.map { dependencyName ->
                KotlinArbitraryDependencyIR(
                    dependencyName,
                    isInMppModule = module.kind
                        .let { it == ModuleKind.multiplatform || it == ModuleKind.target },
                    version = KotlinPlugin::version.propertyValue,
                    dependencyType = DependencyType.TEST
                )
            }
        }

    override val settings: List<ModuleConfiguratorSetting<*, *>> = listOf(framework)
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