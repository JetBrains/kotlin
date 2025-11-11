/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

/**
 * @deprecated This interface was removed.
 */
@ExperimentalAbiValidation
@Deprecated("The interface 'KotlinLegacyAbiDumpTask' was removed.", level = DeprecationLevel.ERROR)
interface KotlinLegacyAbiDumpTask : Task {
    /**
     * The directory to save the whole dump with public ABI for the current [org.gradle.api.Project].
     */
    @get:OutputDirectory
    val dumpDir: Provider<Directory>
}
