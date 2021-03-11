package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class CoroutineStackFrame(context: DefaultExecutionContext) :
        BaseMirror<ObjectReference, MirrorOfCoroutineStackFrame>("kotlinx.coroutines.debug.internal.StackTraceFrame", context) {
    private val callerFrame by FieldMirrorDelegate("callerFrame", this)
    private val stackTraceElement by FieldMirrorDelegate("stackTraceElement", StackTraceElement(context))

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineStackFrame? {
        val callerFrame = callerFrame.mirror(value, context)
        val stackTraceElement = stackTraceElement.mirror(value, context)
        return MirrorOfCoroutineStackFrame(value, callerFrame, stackTraceElement)
    }
}
