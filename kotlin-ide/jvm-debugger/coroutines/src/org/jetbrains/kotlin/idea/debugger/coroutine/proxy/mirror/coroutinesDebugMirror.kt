/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isSubTypeOrSame
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class DebugProbesImpl private constructor(context: DefaultExecutionContext) :
        BaseMirror<ObjectReference, MirrorOfDebugProbesImpl>("kotlinx.coroutines.debug.internal.DebugProbesImpl", context) {
    private val instanceField by FieldDelegate<ObjectReference>("INSTANCE")
    private val instance = instanceField.staticValue()

    private val javaLangListMirror = JavaUtilAbstractCollection(context)
    private val stackTraceElement = StackTraceElement(context)
    private val coroutineInfo =
            CoroutineInfo.instance(this, context) ?: throw IllegalStateException("CoroutineInfo implementation not found.")
    private val debugProbesCoroutineOwner = DebugProbesImplCoroutineOwner(coroutineInfo, context)

    private val isInstalledInCoreMethod by MethodDelegate<BooleanValue>("isInstalled\$kotlinx_coroutines_debug", "()Z")
    private val isInstalledInDebugMethod by MethodDelegate<BooleanValue>("isInstalled\$kotlinx_coroutines_core", "()Z")

    private val enhanceStackTraceWithThreadDumpMethod by MethodMirrorDelegate("enhanceStackTraceWithThreadDump", javaLangListMirror)
    private val dumpMethod by MethodMirrorDelegate("dumpCoroutinesInfo", javaLangListMirror, "()Ljava/util/List;")

    val isInstalled: Boolean by lazy { isInstalled(context) }

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext) =
        MirrorOfDebugProbesImpl(value, instance, isInstalled)

    fun isInstalled(context: DefaultExecutionContext): Boolean =
            isInstalledInDebugMethod.value(instance, context)?.booleanValue() ?:
            isInstalledInCoreMethod.value(instance, context)?.booleanValue()
            ?: throw IllegalStateException("isInstalledMethod not found")

    fun enhanceStackTraceWithThreadDump(
            context: DefaultExecutionContext,
            coroutineInfo: ObjectReference,
            lastObservedStackTrace: ObjectReference
    ): List<MirrorOfStackTraceElement>? {
        instance ?: return emptyList()
        val list = enhanceStackTraceWithThreadDumpMethod.mirror(instance, context, coroutineInfo, lastObservedStackTrace)
                   ?: return emptyList()
        return list.values.mapNotNull { stackTraceElement.mirror(it, context) }
    }

    fun dumpCoroutinesInfo(context: DefaultExecutionContext): List<MirrorOfCoroutineInfo> {
        instance ?: return emptyList()
        val referenceList = dumpMethod.mirror(instance, context) ?: return emptyList()
        return referenceList.values.mapNotNull { coroutineInfo.mirror(it, context) }
    }

    fun getCoroutineInfo(value: ObjectReference?, context: DefaultExecutionContext): MirrorOfCoroutineInfo? {
        val coroutineOwner = debugProbesCoroutineOwner.mirror(value, context)
        return coroutineOwner?.coroutineInfo
    }

    companion object {
        val log by logger

        fun instance(context: DefaultExecutionContext) =
                try {
                    DebugProbesImpl(context)
                }
                catch (e: IllegalStateException) {
                    log.debug("Attempt to access DebugProbesImpl but none found.", e)
                    null
                }
    }
}

class DebugProbesImplCoroutineOwner(private val coroutineInfo: CoroutineInfo, context: DefaultExecutionContext) :
        BaseMirror<ObjectReference, MirrorOfCoroutineOwner>(COROUTINE_OWNER_CLASS_NAME, context) {
    private val infoField by FieldMirrorDelegate("info", DebugCoroutineInfoImpl(context))

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineOwner? {
        val info = infoField.value(value) ?: return null
        if (infoField.isCompatible(info))
            return MirrorOfCoroutineOwner(value, infoField.mirrorOnly(info, context))
        else
            return MirrorOfCoroutineOwner(value, coroutineInfo.mirror(info, context))
    }

    companion object {
        const val COROUTINE_OWNER_CLASS_NAME = "kotlinx.coroutines.debug.internal.DebugProbesImpl\$CoroutineOwner"

        fun instanceOf(value: ObjectReference?) =
                value?.referenceType()?.isSubTypeOrSame(COROUTINE_OWNER_CLASS_NAME) ?: false
    }
}

class DebugCoroutineInfoImpl constructor(context: DefaultExecutionContext) :
        BaseMirror<ObjectReference, MirrorOfCoroutineInfo>("kotlinx.coroutines.debug.internal.DebugCoroutineInfoImpl", context) {
    private val javaLangMirror = JavaLangMirror(context)
    private val stackTraceElement = StackTraceElement(context)

    val lastObservedThread by FieldDelegate<ThreadReference>("lastObservedThread")
    val state by FieldDelegate<ObjectReference>("_state")
    val lastObservedFrame by FieldMirrorDelegate("_lastObservedFrame", WeakReference(context))
    val creationStackBottom by FieldDelegate<ObjectReference>("creationStackBottom")
    val sequenceNumber by FieldDelegate<LongValue>("sequenceNumber")

    val _context by MethodMirrorDelegate("getContext", CoroutineContext(context))
    val getCreationStackTrace by MethodMirrorDelegate("getCreationStackTrace", JavaUtilAbstractCollection(context))

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineInfo? {
        val state = state.value(value)?.let {
            stringValue(it, javaLangMirror.toString, context)
        }

        val coroutineContext = _context.mirror(value, context)
        val creationStackBottom = creationStackBottom.value(value)?.let { CoroutineStackFrame(it, context).mirror() }
        val creationStackTraceMirror = getCreationStackTrace.mirror(value, context)
        val creationStackTrace = creationStackTraceMirror?.values?.mapNotNull { stackTraceElement.mirror(it, context) }
        val lastObservedFrame = lastObservedFrame.mirror(value, context)

        return MirrorOfCoroutineInfo(
                value,
                coroutineContext,
                creationStackBottom,
                sequenceNumber.value(value)?.longValue(),
                null,
                creationStackTrace,
                state,
                lastObservedThread.value(value),
                lastObservedFrame?.reference
        )
    }
}

class CoroutineInfo private constructor(
        private val debugProbesImplMirror: DebugProbesImpl,
        context: DefaultExecutionContext,
        val className: String = AGENT_134_CLASS_NAME
) :
        BaseMirror<ObjectReference, MirrorOfCoroutineInfo>(className, context) {
    private val javaLangMirror = JavaLangMirror(context)
    //private val javaLangListMirror =
    //private val coroutineContextMirror =
    private val stackTraceElement = StackTraceElement(context)
    private val contextFieldRef by FieldMirrorDelegate("context", CoroutineContext(context))
    private val creationStackBottom by FieldDelegate<ObjectReference>("creationStackBottom")
    private val sequenceNumberField by FieldDelegate<LongValue>("sequenceNumber")
    private val creationStackTraceMethod by MethodMirrorDelegate("getCreationStackTrace", JavaUtilAbstractCollection(context))
    private val stateMethod = makeMethod("getState")
    private val lastObservedStackTraceMethod = makeMethod("lastObservedStackTrace")

    private val lastObservedFrameField by FieldDelegate<ObjectReference>("lastObservedFrame")
    private val lastObservedThreadField by FieldDelegate<ThreadReference>("lastObservedThread")

    companion object {
        val log by logger
        private const val AGENT_134_CLASS_NAME = "kotlinx.coroutines.debug.CoroutineInfo"
        private const val AGENT_135_AND_UP_CLASS_NAME = "kotlinx.coroutines.debug.internal.DebugCoroutineInfo"

        fun instance(debugProbesImplMirror: DebugProbesImpl, context: DefaultExecutionContext): CoroutineInfo? {
            val classType = context.findClassSafe(AGENT_135_AND_UP_CLASS_NAME) ?: context.findClassSafe(AGENT_134_CLASS_NAME) ?: return null
            return try {
                CoroutineInfo(debugProbesImplMirror, context, classType.name())
            }
            catch (e: IllegalStateException) {
                log.warn("coroutine-debugger: $classType not found", e)
                null
            }
        }
    }

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineInfo {
        val state = objectValue(value, stateMethod, context)?.let {
            stringValue(it, javaLangMirror.toString, context)
        }
        val coroutineContext = contextFieldRef.mirror(value, context)
        //val creationStackBottomObjectReference = objectValue(value, creationStackBottom)
        val creationStackBottom = creationStackBottom.value(value)?.let { CoroutineStackFrame(it, context).mirror() }
        val sequenceNumber = sequenceNumberField.value(value)?.longValue()
        val creationStackTraceMirror = creationStackTraceMethod.mirror(value, context)
        val creationStackTrace = creationStackTraceMirror?.values?.mapNotNull { stackTraceElement.mirror(it, context) }

        val lastObservedStackTrace = objectValue(value, lastObservedStackTraceMethod, context)
        val enhancedList =
                if (lastObservedStackTrace != null)
                    debugProbesImplMirror.enhanceStackTraceWithThreadDump(context, value, lastObservedStackTrace)
                else emptyList()
        val lastObservedThread = lastObservedThreadField.value(value)
        val lastObservedFrame = lastObservedFrameField.value(value)
        return MirrorOfCoroutineInfo(
                value,
                coroutineContext,
                creationStackBottom,
                sequenceNumber,
                enhancedList,
                creationStackTrace,
                state,
                lastObservedThread,
                lastObservedFrame
        )
    }
}

