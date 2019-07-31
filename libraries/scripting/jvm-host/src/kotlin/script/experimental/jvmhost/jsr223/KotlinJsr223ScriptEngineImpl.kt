/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223

import org.jetbrains.kotlin.cli.common.repl.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluator
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluatorState

// TODO: reimplement without legacy REPL infrastructure

class KotlinJsr223ScriptEngineImpl(
    factory: ScriptEngineFactory,
    baseCompilationConfiguration: ScriptCompilationConfiguration,
    baseEvaluationConfiguration: ScriptEvaluationConfiguration,
    val getScriptArgs: (context: ScriptContext) -> ScriptArgsWithTypes?
) : KotlinJsr223JvmScriptEngineBase(factory), KotlinJsr223InvocableScriptEngine {

    @Volatile
    private var lastScriptContext: ScriptContext? = null

    val jsr223HostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
        jsr223 {
            getScriptContext { lastScriptContext ?: getContext() }
        }
    }

    val compilationConfiguration by lazy {
        ScriptCompilationConfiguration(baseCompilationConfiguration) {
            hostConfiguration(jsr223HostConfiguration)
        }
    }

    val evaluationConfiguration by lazy {
        ScriptEvaluationConfiguration(baseEvaluationConfiguration) {
            hostConfiguration(jsr223HostConfiguration)
        }
    }

    override val replCompiler: ReplCompiler by lazy {
        JvmReplCompiler(compilationConfiguration)
    }

    private val localEvaluator by lazy {
        GenericReplCompilingEvaluatorBase(replCompiler, JvmReplEvaluator(evaluationConfiguration))
    }

    override val replEvaluator: ReplFullEvaluator get() = localEvaluator

    val state: IReplStageState<*> get() = getCurrentState(getContext())

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = replEvaluator.createState(lock)

    override fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = getScriptArgs(context)

    override val invokeWrapper: InvokeWrapper?
        get() = null

    override val backwardInstancesHistory: Sequence<Any>
        get() = getCurrentState(getContext()).asState(JvmReplEvaluatorState::class.java).history.asReversed().asSequence().map { it.item.second }.filterNotNull()

    override val baseClassLoader: ClassLoader
        get() = evaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]!!

    override fun compileAndEval(script: String, context: ScriptContext): Any? {
        // TODO: find a way to pass context to evaluation directly and avoid this hack
        lastScriptContext = context
        return try {
            super.compileAndEval(script, context)
        } finally {
            lastScriptContext = null
        }
    }
}

