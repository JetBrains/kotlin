/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

class ReactJsClientTemplate : JsClientTemplate() {
    override val title: String = KotlinNewProjectWizardBundle.message("module.template.js.react.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.js.react.description")

    @NonNls
    override val id: String = "reactJsClient"

    val useStyledComponents by booleanSetting(
        KotlinNewProjectWizardBundle.message("module.template.react.use.styled.components"),
        GenerationPhase.PROJECT_GENERATION
    ) {
        defaultValue = value(false)
    }

    override val settings: List<TemplateSetting<*, *>> = listOf(useStyledComponents)

    override fun Writer.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = withSettingsOf(module.originalModule) {
        buildList {
            val kotlinVersion = KotlinPlugin.version.propertyValue
            +Dependencies.KOTLIN_REACT(kotlinVersion.version)
            +Dependencies.KOTLIN_REACT_DOM(kotlinVersion.version)
            if (useStyledComponents.reference.settingValue) {
                +Dependencies.KOTLIN_STYLED(kotlinVersion.version)
            }
        }
    }

    override fun Reader.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> =
        withSettingsOf(module.originalModule) {
            buildList {
                val hasKtorServNeighbourTarget = hasKtorServNeighbourTarget(module)
                if (!hasKtorServNeighbourTarget) {
                    +(FileTemplateDescriptor("jsClient/index.html.vm") asResourceOf SourcesetType.main)
                }
                +(FileTemplateDescriptor("$id/reactClient.kt.vm", "client.kt".asPath()) asSrcOf SourcesetType.main)
                +(FileTemplateDescriptor("$id/reactComponent.kt.vm", "welcome.kt".asPath()) asSrcOf SourcesetType.main)

                if (useStyledComponents.reference.settingValue) {
                    +(FileTemplateDescriptor("$id/WelcomeStyles.kt.vm") asSrcOf SourcesetType.main)
                }
            }
        }

    override fun Reader.getAdditionalSettings(module: Module): Map<String, Any> = withSettingsOf(module) {
        mapOf("useStyledComponents" to (useStyledComponents.reference.settingValue))
    }

    private object Dependencies {
        val KOTLIN_REACT = { kotlinVersion: Version ->
            ArtifactBasedLibraryDependencyIR(
                MavenArtifact(Repositories.KOTLIN_JS_WRAPPERS_BINTRAY, "org.jetbrains", "kotlin-react"),
                Versions.JS_WRAPPERS.KOTLIN_REACT(kotlinVersion),
                DependencyType.MAIN
            )
        }
        val KOTLIN_REACT_DOM = { kotlinVersion: Version ->
            ArtifactBasedLibraryDependencyIR(
                MavenArtifact(Repositories.KOTLIN_JS_WRAPPERS_BINTRAY, "org.jetbrains", "kotlin-react-dom"),
                Versions.JS_WRAPPERS.KOTLIN_REACT_DOM(kotlinVersion),
                DependencyType.MAIN
            )
        }
        val KOTLIN_STYLED = { kotlinVersion: Version ->
            ArtifactBasedLibraryDependencyIR(
                MavenArtifact(Repositories.KOTLIN_JS_WRAPPERS_BINTRAY, "org.jetbrains", "kotlin-styled"),
                Versions.JS_WRAPPERS.KOTLIN_STYLED(kotlinVersion),
                DependencyType.MAIN
            )
        }
    }
}
