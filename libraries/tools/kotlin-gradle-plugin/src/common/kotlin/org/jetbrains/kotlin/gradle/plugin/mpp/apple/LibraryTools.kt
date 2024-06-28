/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.utils.runCommand
import java.io.File

internal class LibraryTools(private val logger: Logger? = null) {

    fun createFatLibrary(inputs: List<File>, output: File) {
        val inputLibs = inputs.map { it.canonicalPath }
        val outputLib = output.canonicalPath

        val command = listOf(
            "lipo",
            "-create",
            "-output", outputLib,
        ) + inputLibs

        runCommand(
            command,
            logger = logger
        )
    }

    fun mergeLibraries(inputs: List<File>, output: File) {
        val inputLibs = inputs.map { it.canonicalPath }
        val outputLib = output.canonicalPath

        runCommand(
            listOf(
                "libtool",
                "-static",
                "-o", outputLib
            ) + inputLibs,
            logger = logger
        )
    }
}