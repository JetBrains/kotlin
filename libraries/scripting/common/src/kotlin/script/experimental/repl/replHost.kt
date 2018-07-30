/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.repl

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.runBlocking
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompileConfiguration
import kotlin.script.experimental.api.ScriptDefinition
import kotlin.script.experimental.util.PropertiesCollection

interface ReplCommandProcessor {
    suspend operator fun invoke(
        command: String,
        state: ReplStageState<*>
    ): ResultWithDiagnostics<EvaluationResult>
}

data class ReplCommand(val commandName: String, val processor: ReplCommandProcessor)

interface ReplHostEnvironment : PropertiesCollection {

    companion object : ReplHostEnvironment {

        class Builder internal constructor() : PropertiesCollection.Builder(), ReplHostEnvironment {
            override val properties = data
        }

        fun create(body: Builder.() -> Unit): ReplHostEnvironment = Builder().apply(body)
    }
}

val ReplHostEnvironment.replCommands by PropertiesCollection.key<List<ReplCommand>>()

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
        scriptDefinition: ScriptDefinition,
        additionalConfiguration: ScriptCompileConfiguration? = null, // overrides properties from definition
        replEvaluationEnvironment: ReplEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult>
}
