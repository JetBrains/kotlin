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
    private val javaLangListMirror = JavaUtilAbstractCollection(context)
    private val stackTraceElement = StackTraceElement(context)
    private val coroutineInfo =
        CoroutineInfo.instance(this, context) ?: throw IllegalStateException("CoroutineInfo implementation not found.")
    private val debugProbesCoroutineOwner = DebugProbesImplCoroutineOwner(coroutineInfo, context)
    private val instance = staticObjectValue("INSTANCE")
    private val isInstalledMethod = makeMethod("isInstalled\$kotlinx_coroutines_debug", "()Z")
        ?: makeMethod("isInstalled\$kotlinx_coroutines_core", "()Z") ?: throw IllegalStateException("isInstalledMethod not found")
    val isInstalledValue = booleanValue(instance, isInstalledMethod, context)
    private val enhanceStackTraceWithThreadDumpMethod = makeMethod("enhanceStackTraceWithThreadDump")
    private val dumpMethod = makeMethod("dumpCoroutinesInfo", "()Ljava/util/List;")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfDebugProbesImpl? {
        return MirrorOfDebugProbesImpl(value, instance, isInstalledValue)
    }

    fun enhanceStackTraceWithThreadDump(
        context: DefaultExecutionContext,
        coroutineInfo: ObjectReference,
        lastObservedStackTrace: ObjectReference
    ): List<MirrorOfStackTraceElement>? {
        val listReference =
            staticMethodValue(instance, enhanceStackTraceWithThreadDumpMethod, context, coroutineInfo, lastObservedStackTrace)
        val list = javaLangListMirror.mirror(listReference, context) ?: return null
        return list.values.mapNotNull { stackTraceElement.mirror(it, context) }
    }

    fun dumpCoroutinesInfo(context: DefaultExecutionContext): List<MirrorOfCoroutineInfo> {
        instance ?: return emptyList()
        val coroutinesInfoReference = context.keepReference(objectValue(instance, dumpMethod, context))
        val referenceList = javaLangListMirror.mirror(coroutinesInfoReference, context) ?: return emptyList()
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
            } catch (e: IllegalStateException) {
                log.debug("Attempt to access DebugProbesImpl but none found.", e)
                null
            }
    }
}

class DebugProbesImplCoroutineOwner(private val coroutineInfo: CoroutineInfo, context: DefaultExecutionContext) :
    BaseMirror<ObjectReference, MirrorOfCoroutineOwner>(COROUTINE_OWNER_CLASS_NAME, context) {
    private val debugCoroutineInfoImpl = DebugCoroutineInfoImpl(context)

    private val infoField = makeField("info")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineOwner? {
        val info = objectValue(value, infoField)
        if (debugCoroutineInfoImpl.isCompatible(info)) {
            return MirrorOfCoroutineOwner(value, debugCoroutineInfoImpl.mirror(info, context))
        } else
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
    val _state by FieldDelegate<ObjectReference>("_state")
    val _lastObservedFrame by FieldMirrorDelegate("_lastObservedFrame", WeakReference(context))
    val creationStackBottom by FieldDelegate<ObjectReference>("creationStackBottom")
    val sequenceNumber by FieldDelegate<LongValue>("sequenceNumber")

    val _context by MethodMirrorDelegate("getContext", CoroutineContext(context))
    val getCreationStackTrace by MethodMirrorDelegate("getCreationStackTrace", JavaUtilAbstractCollection(context))

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineInfo? {
        val state = _state.value(value)?.let {
            stringValue(it, javaLangMirror.toString, context)
        }

        val coroutineContext = _context.mirror(value, context)
        val creationStackBottom = creationStackBottom.value(value)?.let { CoroutineStackFrame(it, context).mirror() }
        val creationStackTraceMirror = getCreationStackTrace.mirror(value, context)
        val creationStackTrace = creationStackTraceMirror?.values?.mapNotNull { stackTraceElement.mirror(it, context) }
        val lastObservedFrame = _lastObservedFrame.mirror(value, context)

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
    private val javaLangListMirror = JavaUtilAbstractCollection(context)
    private val coroutineContextMirror = CoroutineContext(context)
    private val stackTraceElement = StackTraceElement(context)
    private val contextFieldRef = makeField("context")
    private val creationStackBottom = makeField("creationStackBottom")
    private val sequenceNumberField = makeField("sequenceNumber")
    private val creationStackTraceMethod = makeMethod("getCreationStackTrace")
    private val stateMethod = makeMethod("getState")
    private val lastObservedStackTraceMethod = makeMethod("lastObservedStackTrace")

    private val lastObservedFrameField = makeField("lastObservedFrame")
    private val lastObservedThreadField = makeField("lastObservedThread")

    companion object {
        val log by logger
        private const val AGENT_134_CLASS_NAME = "kotlinx.coroutines.debug.CoroutineInfo"
        private const val AGENT_135_AND_UP_CLASS_NAME = "kotlinx.coroutines.debug.internal.DebugCoroutineInfo"

        fun instance(debugProbesImplMirror: DebugProbesImpl, context: DefaultExecutionContext): CoroutineInfo? {
            val classType = context.findClassSafe(AGENT_135_AND_UP_CLASS_NAME) ?: context.findClassSafe(AGENT_134_CLASS_NAME) ?: return null
            return try {
                CoroutineInfo(debugProbesImplMirror, context, classType.name())
            } catch (e: IllegalStateException) {
                log.warn("coroutine-debugger: $classType not found", e)
                null
            }
        }
    }

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineInfo {
        val state = objectValue(value, stateMethod, context)?.let {
            stringValue(it, javaLangMirror.toString, context)
        }
        val coroutineContext = coroutineContextMirror.mirror(objectValue(value, contextFieldRef), context)
        val creationStackBottomObjectReference = objectValue(value, creationStackBottom)
        val creationStackBottom =
            creationStackBottomObjectReference?.let { CoroutineStackFrame(creationStackBottomObjectReference, context).mirror() }
        val sequenceNumber = longValue(value, sequenceNumberField)
        val creationStackTraceList = objectValue(value, creationStackTraceMethod, context)
        val creationStackTraceMirror = javaLangListMirror.mirror(creationStackTraceList, context)
        val creationStackTrace = creationStackTraceMirror?.values?.mapNotNull { stackTraceElement.mirror(it, context) }

        val lastObservedStackTrace = objectValue(value, lastObservedStackTraceMethod, context)
        val enhancedList =
            if (lastObservedStackTrace != null)
                debugProbesImplMirror.enhanceStackTraceWithThreadDump(context, value, lastObservedStackTrace)
            else emptyList()
        val lastObservedThread = threadValue(value, lastObservedThreadField)
        val lastObservedFrame = objectValue(value, lastObservedFrameField)
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

class CoroutineStackFrame(value: ObjectReference, context: DefaultExecutionContext) :
    BaseDynamicMirror<MirrorOfCoroutineStackFrame>(value, "kotlin.coroutines.jvm.internal.CoroutineStackFrame", context) {
    private val stackTraceElementMirror = StackTraceElement(context)
    private val callerFrameMethod = findMethod("getCallerFrame")
    private val getStackTraceElementMethod = findMethod("getStackTraceElement")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineStackFrame? {
        val objectReference = objectValue(value, callerFrameMethod, context)
        val callerFrame = if (objectReference is ObjectReference)
            CoroutineStackFrame(objectReference, context).mirror() else null
        val stackTraceElementReference = objectValue(value, getStackTraceElementMethod, context)
        val stackTraceElement = if (stackTraceElementReference is ObjectReference)
            stackTraceElementMirror.mirror(stackTraceElementReference, context)
        else
            null
        return MirrorOfCoroutineStackFrame(value, callerFrame, stackTraceElement)
    }
}

class StackTraceElement(context: DefaultExecutionContext) :
    BaseMirror<ObjectReference, MirrorOfStackTraceElement>("java.lang.StackTraceElement", context) {
    private val declaringClassObjectField = makeField("declaringClass")
    private val moduleNameField = makeField("moduleName")
    private val moduleVersionField = makeField("moduleVersion")
    private val declaringClassField = makeField("declaringClass")
    private val methodNameField = makeField("methodName")
    private val fileNameField = makeField("fileName")
    private val lineNumberField = makeField("lineNumber")
    private val formatField = makeField("format")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfStackTraceElement? {
        val declaringClassObject = objectValue(value, declaringClassObjectField)
        val moduleName = stringValue(value, moduleNameField)
        val moduleVersion = stringValue(value, moduleVersionField)
        val declaringClass = stringValue(value, declaringClassField)
        val methodName = stringValue(value, methodNameField)
        val fileName = stringValue(value, fileNameField)
        val lineNumber = intValue(value, lineNumberField)
        val format = byteValue(value, formatField)
        return MirrorOfStackTraceElement(
            value,
            declaringClassObject,
            moduleName,
            moduleVersion,
            declaringClass,
            methodName,
            fileName,
            lineNumber,
            format
        )
    }
}
