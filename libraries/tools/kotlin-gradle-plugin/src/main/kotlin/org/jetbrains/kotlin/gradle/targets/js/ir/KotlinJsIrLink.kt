/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrType.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrType.PRODUCTION
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import java.io.File

@CacheableTask
open class KotlinJsIrLink : Kotlin2JsCompile() {
    @Input
    lateinit var type: KotlinJsIrType

    // Not check sources, only klib module
    @Internal
    override fun getSource(): FileTree = super.getSource()

    @get:SkipWhenEmpty
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val entryModule: File
        get() = File(
            (taskData.compilation as KotlinJsIrCompilation)
                .output
                .classesDirs
                .asPath
        )

    override fun skipCondition(inputs: IncrementalTaskInputs): Boolean {
        return !inputs.isIncremental && !entryModule.exists()
    }

    @OutputFile
    val outputFileProperty: RegularFileProperty = project.newFileProperty {
        outputFile
    }

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        super.setupCompilerArgs(args, defaultsOnly, ignoreClasspathResolutionErrors)
        when (type) {
            PRODUCTION -> {
                kotlinOptions.configureOptions(ENABLE_DCE, GENERATE_D_TS)
            }
            DEVELOPMENT -> {
                kotlinOptions.configureOptions(GENERATE_D_TS)
            }
        }
    }

    private fun KotlinJsOptions.configureOptions(vararg additionalCompilerArgs: String) {
        freeCompilerArgs += additionalCompilerArgs.toList() +
                PRODUCE_JS +
                "$ENTRY_IR_MODULE=${entryModule.canonicalPath}"
    }
}

enum class KotlinJsIrType {
    PRODUCTION,
    DEVELOPMENT
}