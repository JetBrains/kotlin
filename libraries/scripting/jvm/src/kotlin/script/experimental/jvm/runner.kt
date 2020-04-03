/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.createEvaluationConfigurationFromTemplate
import kotlin.script.experimental.jvm.impl.createScriptFromClassLoader

@Suppress("unused") // script codegen generates a call to it
fun runCompiledScript(scriptClass: Class<*>, vararg args: String) {
    val script = createScriptFromClassLoader(scriptClass.name, scriptClass.classLoader)
    val evaluator = BasicJvmScriptEvaluator()
    val hostConfiguration = script.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]
        ?: defaultJvmScriptingHostConfiguration
    val baseEvaluationConfiguration =
        createEvaluationConfigurationFromTemplate(
            script.compilationConfiguration[ScriptCompilationConfiguration.baseClass]!!,
            hostConfiguration,
            scriptClass.kotlin
        )
    val evaluationConfiguration = ScriptEvaluationConfiguration(baseEvaluationConfiguration) {
        jvm {
            mainArguments(args)
        }
    }
    runScriptSuspend {
        evaluator(script, evaluationConfiguration).onFailure {
            it.reports.forEach(System.err::println)
        }
    }
}

// Copied form kotlin.coroutines.jvm.internal.runSuspend/RunSuspend to create a runner without dependency on the kotlinx.coroutines
private fun runScriptSuspend(block: suspend () -> Unit) {
    val run = RunScriptSuspend()
    block.startCoroutine(run)
    run.await()
}

private class RunScriptSuspend : Continuation<Unit> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    @Suppress("RESULT_CLASS_IN_RETURN_TYPE")
    var result: Result<Unit>? = null

    override fun resumeWith(result: Result<Unit>) = synchronized(this) {
        this.result = result
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") (this as Object).notifyAll()
    }

    fun await() = synchronized(this) {
        while (true) {
            when (val result = this.result) {
                null -> @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") (this as Object).wait()
                else -> {
                    result.getOrThrow() // throw up failure
                    return
                }
            }
        }
    }
}

