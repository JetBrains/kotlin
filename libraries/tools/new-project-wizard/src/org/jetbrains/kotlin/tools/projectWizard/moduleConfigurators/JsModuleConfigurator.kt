/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinBuildSystemPluginIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.RawGradleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleConfigurationData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind

abstract class JSConfigurator : ModuleConfiguratorWithTests() {
    override val moduleType: ModuleType get() = ModuleType.js

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JS
}

object JsSingleplatformModuleConfigurator : JSConfigurator() {
    override val moduleKind = ModuleKind.singleplatformJs
    override val suggestedModuleName = "js"
    override val id = "jsSinglepaltform"
    override val text = "JS for Browser"

    override val canContainSubModules = false

    override fun createKotlinPluginIR(configurationData: ModuleConfigurationData, module: Module): KotlinBuildSystemPluginIR? =
        KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.js,
            version = configurationData.kotlinVersion
        )

    override fun createBuildFileIRs(configurationData: ModuleConfigurationData, module: Module): List<BuildSystemIR> = buildList {
        +RawGradleIR {
            +"kotlin.target.browser { }"
        }
    }
}