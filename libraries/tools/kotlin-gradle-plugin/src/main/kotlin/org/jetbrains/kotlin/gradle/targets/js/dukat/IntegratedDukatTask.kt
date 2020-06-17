/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import javax.inject.Inject

open class DukatTask
@Inject
constructor(
    compilation: KotlinJsCompilation
) : AbstractDukatTask(compilation) {

    override val considerGeneratingFlag: Boolean = true

    @get:OutputDirectory
    override var destDir: File by property {
        compilation.npmProject.externalsDir
    }

    private val executor by lazy {
        DukatExecutor(nodeJs, dts, compilation.npmProject, true, compareInputs = false)
    }

    override fun run() {
        executor.execute()
    }
}