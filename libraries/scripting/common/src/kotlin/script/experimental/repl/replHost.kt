/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.repl

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.runBlocking
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.util.PropertiesCollection

interface ReplCommandProcessor {
    suspend operator fun invoke(
        command: String,
        state: ReplStageState<*>
    ): ResultWithDiagnostics<EvaluationResult>
}

data class ReplCommand(val commandName: String, val processor: ReplCommandProcessor)

interface ReplHostEnvironmentKeys

class ReplHostEnvironment(baseReplHostEnvironments: Iterable<ReplHostEnvironment>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseReplHostEnvironments).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseReplHostEnvironments: ReplHostEnvironment, body: Builder.() -> Unit = {}
    ) : this(baseReplHostEnvironments.asIterable(), body)

    class Builder internal constructor(baseReplHostEnvironments: Iterable<ReplHostEnvironment>) :
        ReplHostEnvironmentKeys,
        PropertiesCollection.Builder(baseReplHostEnvironments)

    companion object : ReplHostEnvironmentKeys
}

val ReplHostEnvironmentKeys.replCommands by PropertiesCollection.key<List<ReplCommand>>()

abstract class ReplHost(
    val environment: ReplHostEnvironment,
    val checker: ReplSnippetChecker,
    val compiler: ReplSnippetCompiler,
    val evaluator: ReplSnippetEvaluator
) {
    open fun <T> runInCoroutineContext(block: suspend CoroutineScope.() -> T): T = runBlocking { block() }

    abstract fun eval(
        snippet: ReplSnippetSource,
        state: ReplStageState<*>,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        replEvaluationEnvironment: ReplEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult>
}
