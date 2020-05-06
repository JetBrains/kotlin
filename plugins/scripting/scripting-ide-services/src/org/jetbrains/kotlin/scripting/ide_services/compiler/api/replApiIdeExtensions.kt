/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler.api

import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode

/**
 * Type of details level for [ReplSymbolInspector.inspect]
 *
 * @property level Numeric representation of details level
 */
enum class DetailsLevel(val level: Int) {
    DEFAULT(0);

    companion object {
        private val intToLevel = values().associateBy { it.level }

        init {
            assert(values().size == intToLevel.size) { "Details levels numeric values are not unique" }
        }

        fun fromInt(level: Int) = intToLevel[level]
    }
}

/**
 * Inspection result representing single descriptor
 *
 * @property fullText Full presentation text
 * @property name Full symbol name
 * @property type Variable type or function signature
 * @property icon Descriptor type in a free form
 * @property documentation KDoc for this declaration (if any)
 */
data class SourceCodeInspectionVariant(
    val fullText: String,
    val name: String,
    val type: String,
    val icon: String,
    val documentation: String,
)

/**
 * Type for [ReplSymbolInspector.inspect]
 */
typealias ReplInspectorResult = Sequence<SourceCodeInspectionVariant>

/**
 * Provides information about symbols in REPL snippets
 */
interface ReplSymbolInspector {

    /**
     * Returns the list of inspection variants in [cursor] position.
     * Generally <b>doesn't change</b> the internal state of implementing object.
     *
     * @param snippet Inspection context
     * @param cursor Cursor position inside, right before or
     *   right after the symbol to be inspected
     * @param configuration Compilation configuration which is used.
     *   Script should be analyzed, but code generation is not performed
     * @param detailsLevel Details level on which inspection is performed
     * @return Sequence of inspection variants:
     *   - Empty sequence if symbol was not resolved
     *   - One-element sequence for properties and types
     *   - Multi-element sequence for functions with several overloads
     */
    suspend fun inspect(
        snippet: SourceCode,
        cursor: SourceCode.Position,
        configuration: ScriptCompilationConfiguration,
        detailsLevel: DetailsLevel = DetailsLevel.DEFAULT,
    ): ResultWithDiagnostics<ReplInspectorResult>
}
