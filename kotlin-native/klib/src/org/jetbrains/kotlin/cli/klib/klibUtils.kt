/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader

/**
 * Note that [libraryPath] can be either absolute, or relative to the current working directory.
 * Other options are not supported.
 */
internal fun loadKlib(libraryPath: String, output: KlibToolOutput): KotlinLibrary? {
    val result = KlibLoader { libraryPaths(libraryPath) }.load()
    if (result.reportLoadingProblemsIfAny { _, message -> output.logError(message) }) return null
    return result.librariesStdlibFirst[0]
}
