/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl

/**
 * @deprecated This class was removed. All its properties have been moved to 'AbiValidationExtension'.
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
interface AbiValidationLegacyDumpExtension {
    /**
     * @deprecated A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.
     */
    val referenceDumpDir: DirectoryProperty

    /**
     * @deprecated A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.
     */
    @Deprecated(
        "A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DEPRECATION_ERROR")
    val legacyDumpTaskProvider: TaskProvider<org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiDumpTask>
        get() = error("A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.")

    /**
     * @deprecated A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.
     */
    @Deprecated(
        "A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DEPRECATION_ERROR")
    val legacyCheckTaskProvider: TaskProvider<org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiCheckTask>
        get() = error("A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.")

    /**
     * @deprecated A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.
     */
    @Deprecated(
        "A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.",
        level = DeprecationLevel.ERROR
    )
    val legacyUpdateTaskProvider: TaskProvider<Task>
        get() = error("A separate block 'legacyDump' was removed. All its properties have been moved to a higher level.")
}
