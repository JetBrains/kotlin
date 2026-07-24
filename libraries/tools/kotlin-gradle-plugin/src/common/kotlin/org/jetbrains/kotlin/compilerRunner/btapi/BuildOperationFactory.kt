/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import java.nio.file.Path
import java.nio.file.Paths

internal interface BuildOperationFactory<out T : BaseCompilationOperation.Builder> {
    fun createOperation(kotlinToolchains: KotlinToolchains): T
}

internal fun extractSourceFiles(freeArgs: List<String>): List<Path> = freeArgs.mapNotNull {
    try {
        Paths.get(it)
    } catch (_: Exception) {
        null
    }
}
