/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.script.experimental.util.LinkedSnippet

/**
 * Compiled snippet type, the most common return type for
 * [ReplCompiler.compile] and [ReplCompiler.lastCompiledSnippet], boxed into
 * [LinkedSnippet] container
 */
typealias CompiledSnippet = CompiledScript

/**
 * Type for [ReplCompleter.complete] return value
 */
typealias ReplCompletionResult = Sequence<SourceCodeCompletionVariant>

/**
 * Type for [ReplCodeAnalyzer.analyze] return value
 */
typealias ReplAnalyzerResult = Sequence<ScriptDiagnostic>

/**
 * REPL Compiler interface which is used for compiling snippets
 * @param CompiledSnippetT Implementation of [CompiledSnippet] which is returned by this compiler
 */
interface ReplCompiler<CompiledSnippetT : CompiledSnippet> {
    /**
     * Reference to the last compile result
     */
    val lastCompiledSnippet: LinkedSnippet<CompiledSnippetT>?

    /**
     * Compiles snippet chain and returns compilation result for the *last* snippet in the chain.
     * Generally <b>changes</b> the internal state of implementing object.
     * @param snippets Chain of snippets to compile
     * @param configuration Compilation configuration which is used
     * @return Compilation result with the last compiled snippet in chain
     */
    suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippetT>>

    /**
     * Compiles snippet and returns compilation result for it.
     * Generally <b>changes</b> the internal state of implementing object.
     * @param snippet Snippet to compile
     * @param configuration Compilation configuration which is used
     * @return Compilation result
     */
    suspend fun compile(
        snippet: SourceCode,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippetT>> = compile(listOf(snippet), configuration)
}

/**
 * Single code completion result
 */
data class SourceCodeCompletionVariant(
    val text: String,
    val displayText: String,
    val tail: String,
    val icon: String
)

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

/**
 * Evaluated snippet type, the most common return type for
 * [ReplEvaluator.eval] and [ReplEvaluator.lastEvaluatedSnippet], boxed into
 * [LinkedSnippet] container
 */
interface EvaluatedSnippet {
    /**
     * Link to the compiled snippet used for the evaluation
     */
    val compiledSnippet: CompiledSnippet

    /**
     * Real evaluation configuration for this snippet
     */
    val configuration: ScriptEvaluationConfiguration

    /**
     * Result of the script evaluation.
     */
    val result: ResultValue
}

/**
 * REPL Evaluator interface which is used for compiled snippets evaluation
 * @param CompiledSnippetT Should be equal to or wider than the corresponding type parameter of compiler.
 *                         Lets evaluator use specific versions of compiled script without type conversion.
 * @param EvaluatedSnippetT Implementation of [EvaluatedSnippet] which is returned by this evaluator
 */
interface ReplEvaluator<CompiledSnippetT : CompiledSnippet, EvaluatedSnippetT : EvaluatedSnippet> {

    /**
     * Reference to the last evaluation result
     */
    val lastEvaluatedSnippet: LinkedSnippet<EvaluatedSnippetT>?

    /**
     * Evaluates compiled snippet and returns result for it.
     * Should assert that snippet sequence is valid.
     * @param snippet Snippet to evaluate.
     * @param configuration Evaluation configuration used.
     * @return Evaluation result
     */
    suspend fun eval(
        snippet: LinkedSnippet<out CompiledSnippetT>,
        configuration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<EvaluatedSnippetT>>
}
