/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDceDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.tasks.IncrementalSyncTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.mapToFile
import javax.inject.Inject

abstract class KotlinBrowserJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val nodeJs = project.rootProject.kotlinNodeJsExtension

    private val propertiesProvider = PropertiesProvider(project)

    init {
        target.compilations.all { compilation ->
            compilation.binaries.all { binary ->
                if (project.locateTask<IncrementalSyncTask>(binary.npmProjectLinkSyncTaskName()) == null) {

                    project.registerTask<DefaultIncrementalSyncTask>(
                        binary.npmProjectLinkSyncTaskName()
                    ) { task ->
                        fun fromLinkTask() {
                            task.from.from(
                                binary.linkTask.flatMap { linkTask ->
                                    linkTask.destinationDirectory
                                }
                            )
                        }
                        when (binary) {
                            is ExecutableWasm -> {
                                if (compilation.isMain() && binary.mode == KotlinJsBinaryMode.PRODUCTION) {
                                    task.from.from(binary.optimizeTask.flatMap { it.outputFileProperty.map { it.asFile.parentFile } })
                                    task.dependsOn(binary.optimizeTask)
                                } else {
                                    fromLinkTask()
                                }
                            }
                            is LibraryWasm -> {
                                if (compilation.isMain() && binary.mode == KotlinJsBinaryMode.PRODUCTION) {
                                    task.from.from(binary.optimizeTask.flatMap { it.outputFileProperty.map { it.asFile.parentFile } })
                                    task.dependsOn(binary.optimizeTask)
                                } else {
                                    fromLinkTask()
                                }
                            }
                            else -> {
                                fromLinkTask()
                            }
                        }

                        task.duplicatesStrategy = DuplicatesStrategy.WARN

                        task.from.from(project.tasks.named(binary.compilation.processResourcesTaskName))

                        task.destinationDirectory.set(binary.compilation.npmProject.dist.mapToFile())
                    }
                }
            }
        }
    }

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
        test.dependsOn(
            nodeJs.npmInstallTaskProvider,
            nodeJs.nodeJsSetupTaskProvider
        )
        test.dependsOn(nodeJs.packageManagerExtension.map { it.postInstallTasks })

        test.dependsOn(binary.npmProjectLinkSyncTaskName())
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        if (test.testFramework == null) {
            test.useKarma {
                useChromeHeadless()
            }
        }

        if (test.enabled) {
            nodeJs.taskRequirements.addTaskRequirements(test)
        }
    }

    override fun mainInputFile(binary: JsIrBinary): Provider<RegularFile> {
        return binary.npmProjectMainFileSyncPath()
    }

    override fun testInputFile(binary: JsIrBinary): Provider<RegularFile> {
        return binary.npmProjectMainFileSyncPath()
    }

    override fun commonWebpackConfig(body: Action<KotlinWebpackConfig>) {
        webpackTask {
            it.webpackConfigApplier(body)
        }
        runTask {
            it.webpackConfigApplier(body)
        }
        testTask {
            it.onTestFrameworkSet {
                if (it is KotlinKarma) {
                    body.execute(it.webpackConfig)
                }
            }
        }
    }

    override fun runTask(body: Action<KotlinWebpack>) {
        subTargetConfigurators.configureEach {
            if (it is WebpackConfigurator) {
                it.configureRun(body)
            }
        }
    }

    override fun webpackTask(body: Action<KotlinWebpack>) {
        subTargetConfigurators.configureEach {
            if (it is WebpackConfigurator) {
                it.configureBuild(body)
            }
        }
    }

    @ExperimentalDceDsl
    override fun dceTask(body: Action<KotlinJsDce>) {
        project.logger.warn("dceTask configuration is useless with IR compiler. Use @JsExport on declarations instead.")
    }

    override fun useWebpack() {
        if (!propertiesProvider.jsBrowserWebpack) {
            subTargetConfigurators.add(WebpackConfigurator(this))
        }
    }

    companion object {
        internal const val WEBPACK_TASK_NAME = "webpack"
    }
}