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
 * A task to dump the public Application Binary Interface (ABI) in files into the [dumpDir] directory.
 *
 * @since 2.1.20
 */
@ExperimentalAbiValidation
interface KotlinLegacyAbiDumpTask : Task {
    /**
     * The directory to save the whole dump with public ABI for the current [org.gradle.api.Project].
     */
    @get:OutputDirectory
    val dumpDir: Provider<Directory>
}
