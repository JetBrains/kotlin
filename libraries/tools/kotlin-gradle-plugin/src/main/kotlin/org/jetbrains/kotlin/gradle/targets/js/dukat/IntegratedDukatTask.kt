/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import java.io.File
import javax.inject.Inject

open class IntegratedDukatTask
@Inject
constructor(
    compilation: KotlinJsCompilation
) : DukatTask(compilation) {

    override val considerGeneratingFlag: Boolean = true

    private val npmProject = compilation.npmProject

    @get:OutputDirectory
    override val destinationDir: File by lazy {
        npmProject.externalsDir
    }

    private val executor
        get() = DukatExecutor(
            nodeJs,
            dts,
            externalsOutputFormat,
            npmProject,
            true,
            compareInputs = false
        )

    @TaskAction
    override fun run() {
        executor.execute(services)
    }
}