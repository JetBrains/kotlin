/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

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
        line.replace(DIAGNOSTIC_REGEX) { match -> match.groupValues[1] }
    }

    private val DIAGNOSTIC_REGEX = Regex("<!.*?!>(.*?)<!>")
}
