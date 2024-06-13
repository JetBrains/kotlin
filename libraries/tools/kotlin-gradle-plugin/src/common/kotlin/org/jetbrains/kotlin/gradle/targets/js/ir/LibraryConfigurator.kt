/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.tasks.Copy
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.DISTRIBUTION_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.dependsOn

class LibraryConfigurator(private val subTarget: KotlinJsIrSubTarget) : SubTargetConfigurator<Copy, Nothing> {
    private val project = subTarget.target.project
    private val target = subTarget.target

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
        val assembleTaskProvider = project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)

        val npmProject = compilation.npmProject

        compilation.binaries
            .matching { it is Library }
            .all { binary ->
                binary as Library

                val mode = binary.mode

                val distributionTask = subTarget.registerSubTargetTask<Copy>(
                    subTarget.disambiguateCamelCased(
                        binary.name,
                        DISTRIBUTION_TASK_NAME
                    )
                ) {
                    if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
                        it.from(project.tasks.named(npmProject.publicPackageJsonTaskName))
                        it.from(binary.linkSyncTask)
                    } else {
                        it.from(binary.linkTask)
                        it.from(project.tasks.named(compilation.processResourcesTaskName))
                    }

                    it.into(binary.distribution.outputDirectory)
                }

                if (mode == KotlinJsBinaryMode.PRODUCTION) {
                    assembleTaskProvider.dependsOn(distributionTask)
                }
            }
    }

    override fun setupRun(compilation: KotlinJsIrCompilation) {
        // do nothing
    }

    override fun configureRun(body: Action<Nothing>) {
        // do nothing
    }

    override fun configureBuild(body: Action<Copy>) {
        // do nothing
    }

}