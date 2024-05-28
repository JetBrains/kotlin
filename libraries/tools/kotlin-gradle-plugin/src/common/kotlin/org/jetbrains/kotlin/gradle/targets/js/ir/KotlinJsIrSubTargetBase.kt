/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

abstract class KotlinJsIrSubTargetBase(target: KotlinJsIrTarget, classifier: String) :
    KotlinJsIrSubTarget(target, classifier) {

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
            binary.mode.name.toLowerCaseAsciiOnly(),
            RUN_TASK_NAME
        )

        locateOrRegisterRunTask(binary, binaryRunName)
    }

    protected abstract fun locateOrRegisterRunTask(binary: JsIrBinary, name: String)

    override fun configureBuild(compilation: KotlinJsIrCompilation) {
        compilation.binaries
            .getIrBinaries(KotlinJsBinaryMode.PRODUCTION)
            .matching { it is Executable }
            .all { productionExecutable ->
                project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(
                    if (productionExecutable is ExecutableWasm) {
                        productionExecutable.optimizeTask
                    } else {
                        productionExecutable.linkTask
                    }
                )
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