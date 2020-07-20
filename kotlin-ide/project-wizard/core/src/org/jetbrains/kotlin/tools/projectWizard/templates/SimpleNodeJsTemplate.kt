/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ArtifactBasedLibraryDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyType
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsNodeTargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.NodeJsSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository.Companion.JCENTER
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module


class SimpleNodeJsTemplate : Template() {
    override val title: String = KotlinNewProjectWizardBundle.message("module.template.simple.nodejs.title")

    override val description: String = KotlinNewProjectWizardBundle.message("module.template.simple.nodejs.description")

    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.js)

    @NonNls
    override val id: String = "simpleNodeJs"

    override fun isApplicableTo(
        reader: Reader,
        module: Module
    ): Boolean = when (module.configurator) {
        JsNodeTargetConfigurator, NodeJsSinglePlatformModuleConfigurator -> true
        else -> false
    }

    val useKotlinxNodejs by booleanSetting(
        KotlinNewProjectWizardBundle.message("module.template.simple.nodejs.use.kotlinx.nodejs"),
        GenerationPhase.PROJECT_GENERATION
    ) {
        defaultValue = value(false)
    }

    override val settings: List<TemplateSetting<*, *>> = listOf(useKotlinxNodejs)

    override fun Writer.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = withSettingsOf(module.originalModule) {
        buildList {
            if (this@SimpleNodeJsTemplate.useKotlinxNodejs.reference.settingValue()) {
                +ArtifactBasedLibraryDependencyIR(
                    MavenArtifact(
                        JCENTER,
                        "org.jetbrains.kotlinx",
                        "kotlinx-nodejs"
                    ),
                    Versions.KOTLINX.KOTLINX_NODEJS,
                    DependencyType.MAIN
                )
            }
        }
    }
}