/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package kotlin.script.experimental.jvmhost.jsr223.base

import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import java.io.Reader
import java.io.Serializable
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.*
import kotlin.reflect.KClass

/**
 * Keep args and arg types together, so as a whole they are present or absent
 */
class ScriptArgsWithTypes(val scriptArgs: Array<out Any?>, val scriptArgsTypes: Array<out KClass<out Any>>) : Serializable {
    init { assert(scriptArgs.size == scriptArgsTypes.size) }
    companion object {
        private val serialVersionUID: Long = 8529357500L
    }
}

const val KOTLIN_SCRIPT_STATE_BINDINGS_KEY = "kotlin.script.state"
const val KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY = "kotlin.script.engine"

interface InvokeWrapper {
    operator fun <T> invoke(body: () -> T): T // e.g. for capturing io
}

abstract class KotlinJsr223JvmScriptEngineBase<State>(
    protected val myFactory: ScriptEngineFactory
) : AbstractScriptEngine(), ScriptEngine, Compilable {

    protected abstract val replCompiler: K2ReplCompiler
    protected abstract val replEvaluator: K2ReplEvaluator

    override fun eval(script: String, context: ScriptContext): Any? = compileAndEval(script, context)

    override fun eval(script: Reader, context: ScriptContext): Any? = compileAndEval(script.readText(), context)

    override fun compile(script: String): CompiledScript = compile(script, getContext())

    override fun compile(script: Reader): CompiledScript = compile(script.readText(), getContext())

    override fun createBindings(): Bindings = SimpleBindings().apply { put(KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY, this) }

    override fun getFactory(): ScriptEngineFactory = myFactory

    protected abstract fun createState(lock: ReentrantReadWriteLock = ReentrantReadWriteLock()): State

    @Suppress("UNCHECKED_CAST")
    protected fun getCurrentState(context: ScriptContext) =
        context.getBindings(ScriptContext.ENGINE_SCOPE)
            .getOrPut(
                KOTLIN_SCRIPT_STATE_BINDINGS_KEY,
                {
                    // TODO: check why createBinding is not called on creating default context, so the engine is not set
                    context.getBindings(ScriptContext.ENGINE_SCOPE).put(KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY, this@KotlinJsr223JvmScriptEngineBase)
                    createState()
                }
            ) as State

    open fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = null

    abstract fun compile(script: String, context: ScriptContext): CompiledScript

    abstract fun compileAndEval(script: String, context: ScriptContext): Any?
}
