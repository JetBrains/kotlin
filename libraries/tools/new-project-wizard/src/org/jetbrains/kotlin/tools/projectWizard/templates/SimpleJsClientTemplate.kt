/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.WizardGradleRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.WizardRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.library.NpmArtifact
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsBrowserTargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsSingleplatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.TemplateInterceptor
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.interceptTemplate

class SimpleJsClientTemplate : Template() {
    override val title: String = "JS client"
    override val htmlDescription: String = title
    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.js)
    override val id: String = "simpleJsClient"

    override fun isApplicableTo(module: Module): Boolean =
        module.configurator == JsBrowserTargetConfigurator
                || module.configurator == JsSingleplatformModuleConfigurator

    val renderEngine by enumSetting<RenderEngine>("Rendering engine", GenerationPhase.PROJECT_GENERATION) {
        defaultValue = RenderEngine.REACT_WITH_STYLED
    }

    override val settings: List<TemplateSetting<*, *>> = listOf(renderEngine)

    override fun ValuesReadingContext.createRunConfigurations(module: ModuleIR): List<WizardRunConfiguration> = buildList {
        if (module.originalModule.kind == ModuleKind.singleplatformJs) {
            +WizardGradleRunConfiguration(
                "BrowserDevelopmentRun in continuous mode",
                "browserDevelopmentRun",
                listOf("--continuous")
            )
            +WizardGradleRunConfiguration(
                "BrowserProductionRun in continuous mode",
                "browserProductionRun",
                listOf("--continuous")
            )
        }
    }

    override fun TaskRunningContext.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = withSettingsOf(module.originalModule) {
        buildList {
            +ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.JCENTER, "org.jetbrains.kotlinx", "kotlinx-html-js"),
                Version.fromString("0.6.12"),
                DependencyType.MAIN
            )

            if (renderEngine.reference.settingValue != RenderEngine.KOTLINX_HTML) {
                +Dependencies.KOTLIN_REACT
                +Dependencies.KOTLIN_REACT_DOM
                +Dependencies.NPM_REACT
                +Dependencies.NPM_REACT_DOM
                if (renderEngine.reference.settingValue == RenderEngine.REACT_WITH_STYLED) {
                    +Dependencies.NPM_REACT_IS
                    +Dependencies.KOTLIN_STYLED
                    +Dependencies.NPM_STYLED_COMPONENTS
                    +Dependencies.NPM_INLINE_STYLE_PREFIXER
                }
            }
        }
    }


    override fun TaskRunningContext.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> =
        withSettingsOf(module.originalModule) {
            buildList {
                val hasKtorServNeighbourTarget = module.safeAs<MultiplatformModuleIR>()
                    ?.neighbourTargetModules()
                    .orEmpty()
                    .any { module ->
                        module.template is KtorServerTemplate
                    }
                if (!hasKtorServNeighbourTarget) {
                    +(FileTemplateDescriptor("$id/index.html.vm") asResourceOf SourcesetType.main)
                }
                if (renderEngine.reference.settingValue == RenderEngine.KOTLINX_HTML) {
                    +(FileTemplateDescriptor("$id/client.kt.vm") asSrcOf SourcesetType.main)
                    +(FileTemplateDescriptor("$id/TestClient.kt.vm", "TestClient.kt".asPath()) asSrcOf SourcesetType.test)
                } else {
                    +(FileTemplateDescriptor("$id/reactClient.kt.vm", "client.kt".asPath()) asSrcOf SourcesetType.main)
                    +(FileTemplateDescriptor("$id/reactComponent.kt.vm", "welcome.kt".asPath()) asSrcOf SourcesetType.main)
                }

                if (renderEngine.reference.settingValue == RenderEngine.REACT_WITH_STYLED) {
                    +(FileTemplateDescriptor("$id/WelcomeStyles.kt.vm") asSrcOf SourcesetType.main)
                }
            }
        }


    override fun createInterceptors(module: ModuleIR): List<TemplateInterceptor> = buildList {
        +interceptTemplate(KtorServerTemplate()) {
            applicableIf { buildFileIR ->
                val tasks = buildFileIR.irsOfTypeOrNull<GradleConfigureTaskIR>() ?: return@applicableIf false
                tasks.none { it.taskAccess.name.endsWith("Jar") }
            }

            interceptAtPoint(template.routes) { value ->
                if (value.isNotEmpty()) return@interceptAtPoint value
                buildList {
                    +value
                    +"""
                    static("/static") {
                        resources()
                    }
                    """.trimIndent()
                }
            }

            interceptAtPoint(template.imports) { value ->
                if (value.isNotEmpty()) return@interceptAtPoint value
                buildList {
                    +value
                    +"io.ktor.http.content.resources"
                    +"io.ktor.http.content.static"
                }
            }

            interceptAtPoint(template.elements) { value ->
                if (value.isNotEmpty()) return@interceptAtPoint value
                buildList {
                    +value
                    +"""
                     div {
                        id = "root"
                     }
                    """.trimIndent()
                    +"""script(src = "/static/$JS_OUTPUT_FILE_NAME") {}"""
                }
            }

            transformBuildFile { buildFileIR ->
                val jsSourcesetName = module.safeAs<MultiplatformModuleIR>()?.name ?: return@transformBuildFile null
                val jvmTarget = buildFileIR.targets.firstOrNull { target ->
                    target.safeAs<DefaultTargetConfigurationIR>()?.targetAccess?.type == ModuleSubType.jvm
                } as? DefaultTargetConfigurationIR ?: return@transformBuildFile null
                val jvmTargetName = jvmTarget.targetName
                val webPackTaskName = "$jsSourcesetName$WEBPACK_TASK_SUFFIX"
                val jvmJarTaskAccess = GradleByNameTaskAccessIR("${jvmTargetName}Jar", "Jar")

                val jvmJarTaskConfiguration = run {
                    val webPackTaskVariable = CreateGradleValueIR(
                        webPackTaskName,
                        GradleByNameTaskAccessIR(webPackTaskName, WEBPACK_TASK_CLASS)
                    )
                    val from = GradleCallIr(
                        "from",
                        listOf(
                            GradleNewInstanceCall(
                                "File",
                                listOf(
                                    GradlePropertyAccessIR("$webPackTaskName.destinationDirectory"),
                                    GradlePropertyAccessIR("$webPackTaskName.outputFileName")
                                )
                            )
                        )
                    )
                    GradleConfigureTaskIR(
                        jvmJarTaskAccess,
                        dependsOn = listOf(GradleByNameTaskAccessIR(webPackTaskName)),
                        irs = listOf(
                            webPackTaskVariable,
                            from
                        )
                    )
                }

                val runTaskConfiguration = run {
                    val taskAccess = GradleByNameTaskAccessIR("run", "JavaExec")
                    val classpath = GradleCallIr("classpath", listOf(jvmJarTaskAccess))
                    GradleConfigureTaskIR(
                        taskAccess,
                        dependsOn = listOf(jvmJarTaskAccess),
                        irs = listOf(classpath)
                    )
                }

                buildFileIR.withIrs(jvmJarTaskConfiguration, runTaskConfiguration)
            }
        }
    }

    override fun TaskRunningContext.getIrsToAddToBuildFile(module: ModuleIR): List<BuildSystemIR> = buildList {
        +RepositoryIR(DefaultRepository.JCENTER)
        if (module is MultiplatformModuleIR) {
            +GradleImportIR("org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack")
            val taskAccessIR = GradleByNameTaskAccessIR(
                "${module.name}$WEBPACK_TASK_SUFFIX",
                WEBPACK_TASK_CLASS
            )

            +GradleConfigureTaskIR(
                taskAccessIR,
                irs = listOf(
                    GradleAssignmentIR(
                        "outputFileName", GradleStringConstIR(JS_OUTPUT_FILE_NAME)
                    )
                )
            )
        }
    }

    companion object {
        private const val JS_OUTPUT_FILE_NAME = "output.js"
        private const val WEBPACK_TASK_CLASS = "KotlinWebpack"
        private const val WEBPACK_TASK_SUFFIX = "BrowserProductionWebpack"
    }

    private object Dependencies {
        val KOTLIN_REACT = ArtifactBasedLibraryDependencyIR(
            MavenArtifact(Repositories.KOTLIN_JS_WRAPPERS_BINTRAY, "org.jetbrains", "kotlin-react"),
            Version.fromString("16.9.0-pre.89-kotlin-1.3.60"),
            DependencyType.MAIN
        )
        val KOTLIN_REACT_DOM = ArtifactBasedLibraryDependencyIR(
            MavenArtifact(Repositories.KOTLIN_JS_WRAPPERS_BINTRAY, "org.jetbrains", "kotlin-react-dom"),
            Version.fromString("16.9.0-pre.89-kotlin-1.3.60"),
            DependencyType.MAIN
        )
        val KOTLIN_STYLED = ArtifactBasedLibraryDependencyIR(
            MavenArtifact(Repositories.KOTLIN_JS_WRAPPERS_BINTRAY, "org.jetbrains", "kotlin-styled"),
            Version.fromString("1.0.0-pre.89-kotlin-1.3.60"),
            DependencyType.MAIN
        )

        val NPM_REACT = ArtifactBasedLibraryDependencyIR(
            NpmArtifact("react"),
            Version.fromString("16.12.0"),
            DependencyType.MAIN
        )
        val NPM_REACT_DOM = ArtifactBasedLibraryDependencyIR(
            NpmArtifact("react-dom"),
            Version.fromString("16.12.0"),
            DependencyType.MAIN
        )
        val NPM_REACT_IS = ArtifactBasedLibraryDependencyIR(
            NpmArtifact("react-is"),
            Version.fromString("16.12.0"),
            DependencyType.MAIN
        )
        val NPM_STYLED_COMPONENTS = ArtifactBasedLibraryDependencyIR(
            NpmArtifact("styled-components"),
            Version.fromString("5.0.0"),
            DependencyType.MAIN
        )
        val NPM_INLINE_STYLE_PREFIXER = ArtifactBasedLibraryDependencyIR(
            NpmArtifact("inline-style-prefixer"),
            Version.fromString("5.1.0"),
            DependencyType.MAIN
        )
    }

    enum class RenderEngine(override val text: String) : DisplayableSettingItem {
        KOTLINX_HTML("Use statically typed Kotlinx.html DSL"),
        REACT("Use Kotlin-wrapped React library"),
        REACT_WITH_STYLED("Use Kotlin-wrapped React framework together with Styled Components"),
    }
}
