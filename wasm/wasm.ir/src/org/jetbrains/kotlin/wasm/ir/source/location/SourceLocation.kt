/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.source.location

sealed class SourceLocation {
    sealed interface WithFileAndLineNumberInformation {
        val file: String
        val line: Int
        val column: Int
    }

    object NoLocation : SourceLocation()
    object NextLocation : SourceLocation()
    object IgnoredLocation : SourceLocation(), WithFileAndLineNumberInformation {
        override val file = "NATIVE_IMPLEMENTATIONS.kt"
        override val line = 0
        override val column = 0
    }

    // Both line and column are zero-based
    data class DefinedLocation(
        override val file: String,
        override val line: Int,
        override val column: Int
    ) : SourceLocation(), WithFileAndLineNumberInformation

    companion object {
        @Suppress("FunctionName", "UNUSED_PARAMETER")
        fun NoLocation(description: String): SourceLocation = NoLocation
    }
}
