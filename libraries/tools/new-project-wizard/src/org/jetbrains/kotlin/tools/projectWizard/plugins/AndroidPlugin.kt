/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.plugins

import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import org.jetbrains.kotlin.tools.projectWizard.core.UNIT_SUCCESS
import org.jetbrains.kotlin.tools.projectWizard.core.checker
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.allIRModules
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GradlePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules

class AndroidPlugin(context: Context) : Plugin(context) {
    val androidSdkPath by pathSetting(
        KotlinNewProjectWizardBundle.message("plugin.android.setting.sdk"),
        neededAtPhase = GenerationPhase.PROJECT_GENERATION
    ) {
        isSavable = true
        isAvailable = isAndroidContainingProject
        shouldExists()
    }

    private val isAndroidContainingProject = checker {
        KotlinPlugin::modules.settingValue
            .withAllSubModules(includeSourcesets = true)
            .any { it.configurator is AndroidModuleConfigurator }
    }

    val addAndroidSdkToLocalProperties by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(GradlePlugin::createLocalPropertiesFile)
        runAfter(KotlinPlugin::createModules)
        isAvailable = isAndroidContainingProject
        withAction {
            if (allIRModules.none { it.originalModule.configurator is AndroidModuleConfigurator }) return@withAction UNIT_SUCCESS
            val path = AndroidPlugin::androidSdkPath.settingValue
            GradlePlugin::localProperties.addValues(
                "sdk.dir" to path
            )
        }
    }
}