/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package kotlin.script.experimental.host

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.api.*

/**
 * The base class for scripting host implementations
 */
abstract class BasicScriptingHost(
    val compiler: ScriptCompiler,
    val evaluator: ScriptEvaluator
) {
    /**
     * The overridable wrapper for executing evaluation in a desired coroutines context
     */
    open fun <T> runInCoroutineContext(block: suspend CoroutineScope.() -> T): T = runBlocking { block() }

    /**
     * The default implementation of the evaluation function
     */
    open fun eval(
        script: SourceCode,
        compilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult> =
        runInCoroutineContext {
            compiler(script, compilationConfiguration).onSuccess {
                evaluator(it, evaluationConfiguration ?: ScriptEvaluationConfiguration.Default)
            }
        }
}
