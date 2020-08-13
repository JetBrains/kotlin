package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

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
