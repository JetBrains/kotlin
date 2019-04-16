/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223

import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.cli.common.repl.renderReplStackTrace
import org.jetbrains.kotlin.utils.tryCreateCallableMapping
import java.lang.reflect.Proxy
import javax.script.Invocable
import javax.script.ScriptException
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.safeCast

interface KotlinJsr223InvocableScriptEngine : Invocable {

    val invokeWrapper: InvokeWrapper?

    val backwardInstancesHistory: Sequence<Any>

    val baseClassLoader: ClassLoader

    fun instancesForInvokeSearch(requestedReceiver: Any) =
        sequenceOf(requestedReceiver) + backwardInstancesHistory.filterNot { it == requestedReceiver }

    override fun invokeFunction(name: String?, vararg args: Any?): Any? {
        if (name == null) throw java.lang.NullPointerException("function name cannot be null")
        return invokeImpl(backwardInstancesHistory, name, args)
    }

    override fun invokeMethod(thiz: Any?, name: String?, vararg args: Any?): Any? {
        if (name == null) throw java.lang.NullPointerException("method name cannot be null")
        if (thiz == null) throw IllegalArgumentException("cannot invoke method on the null object")
        return invokeImpl(instancesForInvokeSearch(thiz), name, args)
    }

    private fun invokeImpl(possibleReceivers: Sequence<Any>, name: String, args: Array<out Any?>): Any? {
        // TODO: cache the method lookups?

        val (fn, mapping) = possibleReceivers.mapNotNull { instance ->
            val candidates = instance::class.functions.filter { it.name == name }
            candidates.findMapping(listOf(instance) + args)
        }.filterNotNull().firstOrNull() ?: throw NoSuchMethodException("no suitable function '$name' found")

        val res = try {
            if (invokeWrapper != null) {
                invokeWrapper!!.invoke {
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
        return proxyInterface(backwardInstancesHistory, clasz)
    }

    override fun <T : Any> getInterface(thiz: Any?, clasz: Class<T>?): T? {
        if (thiz == null) throw IllegalArgumentException("object cannot be null")
        return proxyInterface(instancesForInvokeSearch(thiz), clasz)
    }

    private fun <T : Any> proxyInterface(possibleReceivers: Sequence<Any>, clasz: Class<T>?): T? {
        if (clasz == null) throw IllegalArgumentException("class object cannot be null")
        if (!clasz.isInterface) throw IllegalArgumentException("expecting interface")

        // TODO: cache the method lookups?

        val proxy = Proxy.newProxyInstance(baseClassLoader, arrayOf(clasz)) { _, method, args ->
            invokeImpl(possibleReceivers, method.name, args ?: emptyArray())
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