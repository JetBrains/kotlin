/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrType.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrType.PRODUCTION
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.newFileProperty

open class KotlinJsIrLink : Kotlin2JsCompile() {
    @Input
    lateinit var type: KotlinJsIrType

    @InputFiles
    @SkipWhenEmpty
    override fun getSource(): FileTree {
        // compile from sources, support compile from klib whe it will be supported
        val jsIrCompilation = taskData.compilation as KotlinJsIrCompilation
        return project.files(jsIrCompilation.allSources).asFileTree
    }

    @OutputFile
    val outputFileProperty: RegularFileProperty = project.newFileProperty {
        outputFile
    }

    internal fun configure() {
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
        freeCompilerArgs += additionalCompilerArgs.toList() + PRODUCE_JS
    }
}

enum class KotlinJsIrType {
    PRODUCTION,
    DEVELOPMENT
}