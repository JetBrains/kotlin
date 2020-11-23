/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.WizardGradleRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.WizardRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.TemplateInterceptor
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.interceptTemplate

abstract class JsClientTemplate : Template() {
    override fun isSupportedByModuleType(module: Module, projectKind: ProjectKind): Boolean =
        module.configurator.moduleType == ModuleType.js

    override fun isApplicableTo(
        reader: Reader,
        module: Module
    ): Boolean = when (module.configurator) {
        JsBrowserTargetConfigurator -> true
        BrowserJsSinglePlatformModuleConfigurator -> {
            with(reader) {
                inContextOfModuleConfigurator(module, module.configurator) {
                    JSConfigurator.kind.reference.notRequiredSettingValue == JsTargetKind.APPLICATION
                }
            }
        }
        else -> false
    }

    override fun Reader.createRunConfigurations(module: ModuleIR): List<WizardRunConfiguration> = buildList {
        if (module.originalModule.kind == ModuleKind.singleplatformJsBrowser) {
            +WizardGradleRunConfiguration(
                org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle.message("module.template.js.simple.run.configuration.dev"),
                "browserDevelopmentRun",
                listOf("--continuous")
            )
            +WizardGradleRunConfiguration(
                org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle.message("module.template.js.simple.run.configuration.prod"),
                "browserProductionRun",
                listOf("--continuous")
            )
        }
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

    protected fun hasKtorServNeighbourTarget(module: ModuleIR) =
        module.safeAs<MultiplatformModuleIR>()
            ?.neighbourTargetModules()
            .orEmpty()
            .any { it.template is KtorServerTemplate }

    companion object {
        @NonNls
        private const val JS_OUTPUT_FILE_NAME = "output.js"

        @NonNls
        private const val WEBPACK_TASK_CLASS = "KotlinWebpack"

        @NonNls
        private const val WEBPACK_TASK_SUFFIX = "BrowserProductionWebpack"
    }
}