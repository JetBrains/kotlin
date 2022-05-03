/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateTask

abstract class KotlinJsIrSubTargetBase(target: KotlinJsIrTarget, classifier: String) :
    KotlinJsIrSubTarget(target, classifier) {

    protected val runTaskName = disambiguateCamelCased("run")

    override fun configureRun(compilation: KotlinJsIrCompilation) {
        compilation.binaries
            .withType(JsIrBinary::class.java)
            .matching { it is Executable }
            .all { developmentExecutable ->
                configureRun(developmentExecutable)
            }
    }

    private fun configureRun(binary: JsIrBinary) {
        val binaryRunName = disambiguateCamelCased(
            binary.mode.name.toLowerCase(),
            RUN_TASK_NAME
        )
        locateOrRegisterRunTask(binary, binaryRunName)

        if (binary.mode == KotlinJsBinaryMode.DEVELOPMENT) {
            val runName = disambiguateCamelCased(RUN_TASK_NAME)
            locateOrRegisterRunTask(binary, runName)
        }
    }

    private fun locateOrRegisterRunTask(
        binary: JsIrBinary,
        name: String
    ) {
        val runTask = project.locateTask<NodeJsExec>(name)
        if (runTask == null) {
            val runTaskHolder = NodeJsExec.create(binary.compilation, name) {
                group = taskGroupName
                inputFileProperty.set(
                    project.layout.file(
                        binary.linkSyncTask.map {
                            it.destinationDir
                                .resolve(binary.linkTask.get().outputFileProperty.get().name)
                        }
                    )
                )
            }

            target.runTask.dependsOn(runTaskHolder)
        }
    }

    override fun configureBuild(compilation: KotlinJsIrCompilation) {
        compilation.binaries
            .getIrBinaries(KotlinJsBinaryMode.PRODUCTION)
            .matching { it is Executable }
            .all { productionExecutable ->
                project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(productionExecutable.linkTask)
            }
    }

    override fun configureLibrary(compilation: KotlinJsIrCompilation) {
        super.configureLibrary(compilation)

        compilation.binaries
            .withType(JsIrBinary::class.java)
            .matching { it is Library }
            .all { binary ->
                configureRun(binary)
            }
    }
}