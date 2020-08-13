/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class CoroutineContext(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfCoroutineContext>("kotlin.coroutines.CombinedContext", context) {
    private val coroutineNameRef = CoroutineName(context)
    private val coroutineIdRef = CoroutineId(context)
    private val jobRef = Job(context)
    private val dispatcherRef = CoroutineDispatcher(context)
    private val getContextElement = makeMethod("get")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineContext? {
        val coroutineName = getElementValue(value, context, coroutineNameRef)
        val coroutineId = getElementValue(value, context, coroutineIdRef)
        val job = getElementValue(value, context, jobRef)
        val dispatcher = getElementValue(value, context, dispatcherRef)
        return MirrorOfCoroutineContext(value, coroutineName, coroutineId, dispatcher, job)
    }

    private fun <T> getElementValue(value: ObjectReference, context: DefaultExecutionContext, keyProvider: ContextKey<T>): T? {
        val key = keyProvider.key() ?: return null
        val elementValue = objectValue(value, getContextElement, context, key) ?: return null
        return keyProvider.mirror(elementValue, context)
    }
}

abstract class ContextKey<T>(name: String, context: DefaultExecutionContext) : BaseMirror<T>(name, context) {
    abstract fun key(): ObjectReference?
}

class CoroutineName(context: DefaultExecutionContext) : ContextKey<String>("kotlinx.coroutines.CoroutineName", context) {
    val key = staticObjectValue("Key")
    private val getNameRef = makeMethod("getName")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): String? {
        return stringValue(value, getNameRef, context)
    }

    override fun key() = key
}

class CoroutineId(context: DefaultExecutionContext) : ContextKey<Long>("kotlinx.coroutines.CoroutineId", context) {
    private val key = staticObjectValue("Key")
    private val getIdRef = makeMethod("getId")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): Long? {
        return longValue(value, getIdRef, context)
    }

    override fun key() = key
}

class Job(context: DefaultExecutionContext) : ContextKey<ObjectReference>("kotlinx.coroutines.Job\$Key", context) {
    val key = staticObjectValue("\$\$INSTANCE")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): ObjectReference? {
        return value
    }

    override fun key() = key
}


class CoroutineDispatcher(context: DefaultExecutionContext) : ContextKey<String>("kotlinx.coroutines.CoroutineDispatcher", context) {
    private val key = staticObjectValue("Key")
    private val jlm = JavaLangMirror(context)

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): String? {
        return jlm.string(value, context)
    }

    override fun key() = key
}
