/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import java.io.File

/**
 * Prepare `this` to be an output for the task:
 * * delete if exists
 * * make sure all parent directories exist
 */
internal fun File.prepareAsOutput() {
    val deleted = deleteRecursively()
    check(deleted) { "Failed to delete $path" }
    // Gradle automatically should have created parent directory for all output files/directories.
    check(parentFile.exists()) { "Parent directory for $path does not exist" }
}