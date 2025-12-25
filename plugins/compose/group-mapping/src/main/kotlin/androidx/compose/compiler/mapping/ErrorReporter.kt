/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping

interface ErrorReporter {
    fun reportAnalysisError(e: Throwable)
    fun reportClassReadError(fileName: String, e: Throwable)

    companion object Default : ErrorReporter {
        override fun reportAnalysisError(e: Throwable) {
            throw e
        }

        override fun reportClassReadError(fileName: String, e: Throwable) {
            error("Failed to read $fileName: ${e.message}")
        }
    }
}