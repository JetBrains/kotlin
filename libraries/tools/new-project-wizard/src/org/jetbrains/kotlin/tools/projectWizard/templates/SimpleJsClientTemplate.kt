/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JsBrowserTargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.TemplateInterceptor
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.interceptTemplate

class SimpleJsClientTemplate : Template() {
    override val title: String = "Simple JS client"
    override val htmlDescription: String = title
    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.js)
    override val sourcesetTypes: Set<SourcesetType> = setOf(SourcesetType.main)
    override val id: String = "simpleJsClient"

    override fun isApplicableTo(sourceset: Sourceset): Boolean =
        sourceset.parent?.configurator == JsBrowserTargetConfigurator

    override fun TaskRunningContext.getRequiredLibraries(sourceset: SourcesetIR): List<DependencyIR> =
        buildList {
            +ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.JCENTER, "org.jetbrains.kotlinx", "kotlinx-html-js"),
                Version.fromString("0.6.12"),
                DependencyType.MAIN
            )
        }

    override fun TaskRunningContext.getFileTemplates(sourceset: SourcesetIR): List<FileTemplateDescriptor> = listOf(
        FileTemplateDescriptor("$id/client.kt.vm", sourcesPath("client.kt"))
    )

    override fun createInterceptors(sourceset: SourcesetIR): List<TemplateInterceptor> = buildList {
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
                    +"""script(src = "/static/output.js") {}"""
                }
            }

            transformBuildFile { buildFileIR ->
                val jsSourcesetName = sourceset.safeAs<SourcesetModuleIR>()?.targetName ?: return@transformBuildFile null
                val jvmTarget = buildFileIR.targets.firstOrNull { target ->
                    target.safeAs<DefaultTargetConfigurationIR>()?.targetAccess?.type == ModuleSubType.jvm
                } as? DefaultTargetConfigurationIR ?: return@transformBuildFile null
                val jvmTargetName = jvmTarget.targetName
                val webPackTaskName = "${jsSourcesetName}BrowserWebpack"
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


    override fun TaskRunningContext.getIrsToAddToBuildFile(sourceset: SourcesetIR): List<BuildSystemIR> = buildList {
        +RepositoryIR(DefaultRepository.JCENTER)
        if (sourceset is SourcesetModuleIR) {
            +GradleImportIR("org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack")
            val taskAccessIR = GradleByNameTaskAccessIR(
                "${sourceset.targetName}BrowserWebpack",
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
    }
}