/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.tasks.Copy
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.RUN_TASK_NAME
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

class JsEnvironmentConfigurator(private val subTarget: KotlinJsIrSubTargetJsEnvWithRunTask) : SubTargetConfigurator<Copy, Nothing> {
    private val project = subTarget.target.project

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
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

    override fun setupRun(compilation: KotlinJsIrCompilation) {
        compilation.binaries
            .withType(JsIrBinary::class.java)
            .matching { it is Executable }
            .all { developmentExecutable ->
                configureBinaryRun(developmentExecutable)
            }
    }

    override fun configureRun(body: Action<Nothing>) {
        // do nothing
    }

    override fun configureBuild(body: Action<Copy>) {
        // do nothing
    }

    private fun configureBinaryRun(binary: JsIrBinary) {
        val binaryRunName = subTarget.disambiguateCamelCased(
            binary.mode.name.toLowerCaseAsciiOnly(),
            RUN_TASK_NAME
        )
        subTarget.locateOrRegisterRunTask(binary, binaryRunName)
    }
}