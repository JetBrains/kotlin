/*
 * Copyright 2010-2025 JetBrains s.r.o.
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

import org.jetbrains.kotlin.utils.tryCreateCallableMapping
import java.lang.reflect.Proxy
import javax.script.Invocable
import javax.script.ScriptException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.safeCast
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.ScriptExecutionWrapper
import kotlin.script.experimental.api.scriptExecutionWrapper
import kotlin.script.experimental.jvmhost.jsr223.K2ReplState
import kotlin.script.experimental.jvmhost.jsr223.renderReplStackTrace
import kotlin.script.experimental.util.toList

data class EvalClassWithInstanceAndLoader(
    val klass: KClass<*>, val instance: Any?, val invokeWrapper: ScriptExecutionWrapper<Any?>?
)

@Suppress("unused") // used externally (kotlin.script.utils)
interface KotlinJsr223JvmInvocableScriptEngine : Invocable {

    val state: K2ReplState // The Invokable interface do not allow Context/Bindings substitution, so state is supplied via property

    private fun prioritizedHistory(receiverClass: KClass<*>?, receiverInstance: Any?): List<EvalClassWithInstanceAndLoader> {
        @Suppress("UNCHECKED_CAST")
        val wrapper =
            state.evaluator.lastEvaluatedSnippet?.get()?.configuration?.get(ScriptEvaluationConfiguration.scriptExecutionWrapper)
                    as? ScriptExecutionWrapper<Any?>
        return state.evaluator.lastEvaluatedSnippet.toList()
            .filter { it.result.scriptInstance != null }
            .map {
                @Suppress("UNCHECKED_CAST")
                EvalClassWithInstanceAndLoader(
                    it.result.scriptClass!!,
                    it.result.scriptInstance,
                    it.configuration[ScriptEvaluationConfiguration.scriptExecutionWrapper] as? ScriptExecutionWrapper<Any?>
                )
            }
            .let { history ->
                if (receiverInstance != null) {
                    val receiverKlass = receiverClass ?: receiverInstance::class
                    val receiverInHistory =
                        history.find { it.instance == receiverInstance }
                            ?: EvalClassWithInstanceAndLoader(receiverKlass, receiverInstance, wrapper)
                    listOf(receiverInHistory) + history.filterNot { it == receiverInHistory }
                } else {
                    history
                }
            }
    }

    override fun invokeFunction(name: String?, vararg args: Any?): Any? {
        if (name == null) throw java.lang.NullPointerException("function name cannot be null")
        return invokeImpl(prioritizedHistory(null, null), name, args)
    }

    override fun invokeMethod(thiz: Any?, name: String?, vararg args: Any?): Any? {
        if (name == null) throw java.lang.NullPointerException("method name cannot be null")
        if (thiz == null) throw IllegalArgumentException("cannot invoke method on the null object")
        return invokeImpl(prioritizedHistory(thiz::class, thiz), name, args)
    }

    private fun invokeImpl(prioritizedCallOrder: List<EvalClassWithInstanceAndLoader>, name: String, args: Array<out Any?>): Any? {
        // TODO: cache the method lookups?

        val [fn, mapping, invokeWrapper] = prioritizedCallOrder.firstNotNullOfOrNull { (klass, instance, invokeWrapper) ->
            val candidates = klass.functions.filter { it.name == name }
            candidates.findMapping(listOf(instance) + args)?.let {
                Triple(it.first, it.second, invokeWrapper)
            }
        } ?: throw NoSuchMethodException("no suitable function '$name' found")

        val res = try {
            if (invokeWrapper != null) {
                invokeWrapper.invoke {
                    fn.callBy(mapping)
                }
            } else {
                fn.callBy(mapping)
            }
        } catch (e: Throwable) {
            // ignore everything in the stack trace until this constructor call
            throw ScriptException(renderReplStackTrace(e.cause!!, startFromMethodName = fn.name))
        }
        return if (fn.returnType.classifier == Unit::class) Unit else res
    }


    override fun <T : Any> getInterface(clasz: Class<T>?): T? {
        return proxyInterface(null, clasz)
    }

    override fun <T : Any> getInterface(thiz: Any?, clasz: Class<T>?): T? {
        if (thiz == null) throw IllegalArgumentException("object cannot be null")
        return proxyInterface(thiz, clasz)
    }

    private fun <T : Any> proxyInterface(thiz: Any?, clasz: Class<T>?): T? {
        if (state.evaluator.lastEvaluatedSnippet == null) throw IllegalStateException("no script")
        val priority = prioritizedHistory(thiz?.javaClass?.kotlin, thiz)

        if (clasz == null) throw IllegalArgumentException("class object cannot be null")
        if (!clasz.isInterface) throw IllegalArgumentException("expecting interface")

        // TODO: cache the method lookups?

        val proxy = Proxy.newProxyInstance(Thread.currentThread().contextClassLoader, arrayOf(clasz)) { _, method, args ->
            invokeImpl(priority, method.name, args ?: emptyArray())
        }
        return clasz.kotlin.safeCast(proxy)
    }
}

private fun Iterable<KFunction<*>>.findMapping(args: List<Any?>): Pair<KFunction<*>, Map<KParameter, Any?>>? {
    for (fn in this) {
        val mapping = tryCreateCallableMapping(fn, args)
        if (mapping != null) return fn to mapping
    }
    return null
}
