/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.script.experimental.util.PropertiesCollection

/**
 * Type for [ReplCompleter.complete] return value
 */
typealias ReplCompletionResult = Sequence<SourceCodeCompletionVariant>

/**
 * Single code completion result
 */
data class SourceCodeCompletionVariant(
    val text: String,
    val displayText: String,
    val tail: String,
    val icon: String,
    val deprecationLevel: DeprecationLevel? = null,
)

interface ReplAnalyzerResultKeys

/**
 * The container for analysis results data returned from the [ReplCodeAnalyzer.analyze]
 */
class ReplAnalyzerResult(baseConfigurations: Iterable<ReplAnalyzerResult>, body: Builder.() -> Unit = {}) :
    PropertiesCollection(Builder(baseConfigurations).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)

    class Builder internal constructor(baseConfigurations: Iterable<ReplAnalyzerResult>) :
        ReplAnalyzerResultKeys,
        PropertiesCollection.Builder(baseConfigurations)

    companion object : ReplAnalyzerResultKeys
}

/**
 * Script compile-time errors and warning with their locations
 */
val ReplAnalyzerResultKeys.analysisDiagnostics by PropertiesCollection.key<Sequence<ScriptDiagnostic>>(emptySequence())

/**
 * String representing snippet return value, or null, if script returns nothing
 */
val ReplAnalyzerResultKeys.renderedResultType by PropertiesCollection.key<String?>()

/**
 * Interface for REPL context completion
 */
interface ReplCompleter {

    /**
     * Returns the list of possible reference variants in [cursor] position.
     * Generally <b>doesn't change</b> the internal state of implementing object.
     * @param snippet Completion context
     * @param cursor Cursor position in which completion variants should be calculated
     * @param configuration Compilation configuration which is used. Script should be analyzed, but code generation is not performed
     * @return List of reference variants
     */
    suspend fun complete(
        snippet: SourceCode,
        cursor: SourceCode.Position,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<ReplCompletionResult>
}

/**
 * Interface for REPL syntax and semantic analysis
 */
interface ReplCodeAnalyzer {

    /**
     * Reports compilation errors and warnings in the given [snippet]
     * @param snippet Code to analyze
     * @param cursor Current cursor position. May be used by implementation for suppressing errors and warnings near it.
     * @param configuration Compilation configuration which is used. Script should be analyzed, but code generation is not performed
     * @return List of diagnostic messages
     */
    suspend fun analyze(
        snippet: SourceCode,
        cursor: SourceCode.Position,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<ReplAnalyzerResult>
}