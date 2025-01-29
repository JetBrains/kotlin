/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.runtime

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.JsEnvironmentConfigurator
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.RUN_TASK_NAME
import org.jetbrains.kotlin.gradle.utils.getFile

@ExperimentalWasmDsl
internal class CommonEnvironmentConfigurator(
    private val subTargetSpecific: KotlinCommonSubTarget,
) : JsEnvironmentConfigurator<Exec>(subTargetSpecific) {

    @OptIn(ExperimentalStdlibApi::class)
    override fun configureBinaryRun(binary: JsIrBinary): TaskProvider<Exec> {
        val binaryRunName = subTarget.disambiguateCamelCased(
            binary.mode.name.lowercase(),
            RUN_TASK_NAME
        )

        return project.tasks.register(binaryRunName, Exec::class.java).also {
            it.configure { task ->
                task.dependsOn(subTargetSpecific.setupTask)

                task.group = subTargetSpecific.name

                task.description = "Run ${subTargetSpecific.name}"

                task.dependsOn(binary.linkTask)

                val binaryInputFile =
                    binary.mainFile.map { it.asFile.parentFile.resolve(it.asFile.nameWithoutExtension + ".wasm") }
                val workingDir = binary.outputDirBase

                val executableProvider = subTargetSpecific.envSpec.executable

                val args = subTargetSpecific.runArgs

                task.doFirst {
                    it as Exec
                    it.executable = executableProvider.get()

                    val isolationDir = workingDir.getFile().resolve("static/${it.name}").also {
                        it.mkdirs()
                    }

                    it.workingDir = isolationDir

                    it.args(
                        args.get().provideArgs(
                            isolationDir.toPath(),
                            binaryInputFile.get().toPath()
                        )
                    )
                }

            }
        }
    }
}