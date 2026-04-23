/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration

fun CompilerConfiguration.reportCompilationErrorAndThrow(message: String): Nothing {
    report(CliDiagnostics.KONAN_COMPILATION_ERROR, message)
    throw KonanCompilationException()
}