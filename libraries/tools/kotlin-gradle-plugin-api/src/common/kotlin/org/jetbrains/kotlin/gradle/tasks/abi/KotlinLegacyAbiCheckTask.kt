/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

/**
 * A task to check the Applicaton Binary Interface (ABI) from dump files located in [referenceDir] with ABI in dump files in [actualDir].
 *
 * The files are compared as text files. If the contents differ, the task fails with an error.
 *
 * @since 2.1.20
 */
@ExperimentalAbiValidation
interface KotlinLegacyAbiCheckTask : Task {
    /**
     * A directory containing ABI dumps for the previous project version.
     */
    @get:InputFiles // InputFiles is used so as not to fail with an error if reference directory does not exist https://github.com/gradle/gradle/issues/2016
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val referenceDir: Provider<Directory>

    /**
     * A directory containing ABI dumps for the current project's classes.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val actualDir: Provider<Directory>
}
