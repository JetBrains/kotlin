/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates


import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.WizardGradleRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.WizardRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.TemplateInterceptor
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.interceptTemplate
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin

class SimpleJsClientTemplate : Template() {
    override val title: String = KotlinNewProjectWizardBundle.message("module.template.js.simple.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.js.simple.description")

    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.js)

    @NonNls
    override val id: String = "simpleJsClient"

    override fun isApplicableTo(
        reader: Reader,
        module: Module
    ): Boolean = when (module.configurator) {
        JsBrowserTargetConfigurator -> true
        BrowserJsSinglePlatformModuleConfigurator -> {
            with(reader) {
                withSettingsOf(module, module.configurator) {
                    JSConfigurator.kind.reference.notRequiredSettingValue == JsTargetKind.APPLICATION
                }
            }
        }
        else -> false
    }

    val renderEngine by enumSetting<RenderEngine>(
        KotlinNewProjectWizardBundle.message("module.template.js.simple.setting.rendering.engine"),
        GenerationPhase.PROJECT_GENERATION
    ) {
        defaultValue = value(RenderEngine.REACT_WITH_STYLED)
    }

    override val settings: List<TemplateSetting<*, *>> = listOf(renderEngine)

    override fun Reader.createRunConfigurations(module: ModuleIR): List<WizardRunConfiguration> = buildList {
        if (module.originalModule.kind == ModuleKind.singleplatformJsBrowser) {
            +WizardGradleRunConfiguration(
                KotlinNewProjectWizardBundle.message("module.template.js.simple.run.configuration.dev"),
                "browserDevelopmentRun",
                listOf("--continuous")
            )
            +WizardGradleRunConfiguration(
                KotlinNewProjectWizardBundle.message("module.template.js.simple.run.configuration.prod"),
                "browserProductionRun",
                listOf("--continuous")
            )
        }
    }

    override fun Writer.getRequiredLibraries(module: ModuleIR): List<DependencyIR> = withSettingsOf(module.originalModule) {
        buildList {
            +ArtifactBasedLibraryDependencyIR(
                MavenArtifact(Repositories.KOTLINX, "org.jetbrains.kotlinx", "kotlinx-html-js"),
                Versions.KOTLINX.KOTLINX_HTML(KotlinPlugin::version.propertyValue.version),
                DependencyType.MAIN
            )

            val kotlinVersion = KotlinPlugin::version.propertyValue
            if (renderEngine.reference.settingValue != RenderEngine.KOTLINX_HTML) {
                +Dependencies.KOTLIN_REACT(kotlinVersion.version)
                +Dependencies.KOTLIN_REACT_DOM(kotlinVersion.version)
                if (renderEngine.reference.settingValue == RenderEngine.REACT_WITH_STYLED) {
                    +Dependencies.KOTLIN_STYLED(kotlinVersion.version)
                }
            }
        }
    }


    override fun Reader.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> =
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

    override fun Reader.getAdditionalSettings(module: Module): Map<String, Any> = withSettingsOf(module) {
        mapOf("useStyledComponents" to (renderEngine.reference.settingValue == RenderEngine.REACT_WITH_STYLED))
    }

    override fun createInterceptors(module: ModuleIR): List<TemplateInterceptor> = buildList {
        +interceptTemplate(KtorServerTemplate()) {
            applicableIf { buildFileIR ->
                val tasks = buildFileIR.irsOfTypeOrNull<GradleConfigureTaskIR>() ?: return@applicableIf false
                tasks.none { it.taskAccess.safeAs<GradleByNameTaskAccessIR>()?.name?.endsWith("Jar") == true }
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

    override fun Writer.getIrsToAddToBuildFile(module: ModuleIR): List<BuildSystemIR> = buildList {
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
        @NonNls
        private const val JS_OUTPUT_FILE_NAME = "output.js"

        @NonNls
        private const val WEBPACK_TASK_CLASS = "KotlinWebpack"

        @NonNls
        private const val WEBPACK_TASK_SUFFIX = "BrowserProductionWebpack"
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

    enum class RenderEngine(@Nls override val text: String) : DisplayableSettingItem {
        KOTLINX_HTML(KotlinNewProjectWizardBundle.message("module.template.js.simple.setting.rendering.kotlinx.html")),
        REACT(KotlinNewProjectWizardBundle.message("module.template.js.simple.setting.rendering.react")),
        REACT_WITH_STYLED(KotlinNewProjectWizardBundle.message("module.template.js.simple.setting.rendering.react.styled"))
    }
}
