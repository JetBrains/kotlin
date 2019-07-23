/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import java.io.File

class DukatExecutor(
    val compilation: KotlinJsCompilation,
    val dTsFiles: Collection<File>,
    val destDir: File,
    val qualifiedPackageName: String? = null,
    val jsInteropJvmEngine: String? = null,
    val operation: String = "Generating Kotlin/JS external declarations"
) {
    fun execute() {
        compilation.target.project.execWithProgress(operation) { exec ->
            val args = mutableListOf<String>()

            val qualifiedPackageName = qualifiedPackageName
            if (qualifiedPackageName != null) {
                args.add("-p")
                args.add(qualifiedPackageName)
            }

            args.add("-d")
            args.add(destDir.absolutePath)

            val jsInteropJvmEngine = jsInteropJvmEngine
            if (jsInteropJvmEngine != null) {
                args.add("-js")
                args.add(jsInteropJvmEngine)
            }

            args.addAll(dTsFiles.map { it.absolutePath })

            compilation.npmProject.useTool(exec, ".bin/dukat", *args.toTypedArray())
        }
    }
}