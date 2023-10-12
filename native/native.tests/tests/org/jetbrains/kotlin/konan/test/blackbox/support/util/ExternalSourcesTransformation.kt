/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCodegenBoxTest

/**
 * This relates only to external codegen tests (see [AbstractNativeCodegenBoxTest]) that may have their own source transformers.
 */

internal typealias ExternalSourceTransformer = (/* file contents */ String) -> /* patched file contents */ String
internal typealias ExternalSourceTransformers = List<ExternalSourceTransformer>

internal object DiagnosticsRemovingSourceTransformer : ExternalSourceTransformer {
    override fun invoke(source: String) = source.lineSequence().joinToString("\n") { line ->
        // Remove all diagnostic parameters from the text. Examples:
        //   <!NO_TAIL_CALLS_FOUND!>, <!NON_TAIL_RECURSIVE_CALL!>, <!>.
        // Removal must be done per source line, since doing it for whole file causes issues under Windows
        clearTextFromDiagnosticMarkup(line)
    }
}
