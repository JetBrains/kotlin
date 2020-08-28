/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ArtifactBasedLibraryDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.DependencyType
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
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

    val useReactRouterDom by booleanSetting(
        KotlinNewProjectWizardBundle.message("module.template.react.use.react.router.dom"),
        GenerationPhase.PROJECT_GENERATION
    ) {
        defaultValue = value(false)
    }

    val useReactRedux by booleanSetting(
        KotlinNewProjectWizardBundle.message("module.template.react.use.react.redux"),
        GenerationPhase.PROJECT_GENERATION
    ) {
        defaultValue = value(false)
    }

    override val settings: List<TemplateSetting<*, *>> =
        listOf(
            useStyledComponents,
            useReactRouterDom,
            useReactRedux
        )

    override fun Writer.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = withSettingsOf(module.originalModule) {
        buildList {
            val kotlinVersion = KotlinPlugin.version.propertyValue
            +Dependencies.KOTLIN_REACT(kotlinVersion.version)
            +Dependencies.KOTLIN_REACT_DOM(kotlinVersion.version)
            if (useStyledComponents.reference.settingValue) {
                +Dependencies.KOTLIN_STYLED(kotlinVersion.version)
            }
            if (useReactRouterDom.reference.settingValue) {
                +Dependencies.KOTLIN_REACT_ROUTER_DOM(kotlinVersion.version)
            }
            if (useReactRedux.reference.settingValue) {
                +Dependencies.KOTLIN_REDUX(kotlinVersion.version)
                +Dependencies.KOTLIN_REACT_REDUX(kotlinVersion.version)
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
        val KOTLIN_REACT = wrapperDependency(
            "kotlin-react",
            Versions.JS_WRAPPERS.KOTLIN_REACT
        )

        val KOTLIN_REACT_DOM = wrapperDependency(
            "kotlin-react-dom",
            Versions.JS_WRAPPERS.KOTLIN_REACT_DOM
        )

        val KOTLIN_STYLED = wrapperDependency(
            "kotlin-styled",
            Versions.JS_WRAPPERS.KOTLIN_STYLED
        )

        val KOTLIN_REACT_ROUTER_DOM = wrapperDependency(
            "kotlin-react-router-dom",
            Versions.JS_WRAPPERS.KOTLIN_REACT_ROUTER_DOM
        )

        val KOTLIN_REDUX = wrapperDependency(
            "kotlin-redux",
            Versions.JS_WRAPPERS.KOTLIN_REDUX
        )

        val KOTLIN_REACT_REDUX = wrapperDependency(
            "kotlin-react-redux",
            Versions.JS_WRAPPERS.KOTLIN_REACT_REDUX
        )

        private fun wrapperDependency(artifact: String, version: (Version) -> Version) = { kotlinVersion: Version ->
            ArtifactBasedLibraryDependencyIR(
                MavenArtifact(Repositories.KOTLIN_JS_WRAPPERS_BINTRAY, "org.jetbrains", artifact),
                version(kotlinVersion),
                DependencyType.MAIN
            )
        }
    }
}
