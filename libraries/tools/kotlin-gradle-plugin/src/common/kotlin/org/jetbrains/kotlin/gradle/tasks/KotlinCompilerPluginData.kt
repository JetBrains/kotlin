/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.File

@InternalKotlinGradlePluginApi
data class KotlinCompilerPluginData(
    @get:Classpath
    val classpath: FileCollection,

    @get:Internal
    val options: CompilerPluginOptions,

    /**
     * Used only for Up-to-date checks
     */
    @get:Nested
    val inputsOutputsState: InputsOutputsState
) {
    data class InputsOutputsState(
        @get:Input
        val inputs: Map<String, String>,

        @get:InputFiles
        @get:IgnoreEmptyDirectories
        @get:NormalizeLineEndings
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val inputFiles: Set<File>,

        @get:OutputFiles
        val outputFiles: Set<File>
    )
}