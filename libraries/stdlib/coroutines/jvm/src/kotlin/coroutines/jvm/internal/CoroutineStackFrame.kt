/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.jvm.internal

import kotlin.coroutines.*

/**
 * Represents one frame in the coroutine call stack for debugger.
 * This interface is implemented by compiler-generated implementations of
 * [Continuation] interface.
 */
public interface CoroutineStackFrame {
    /**
     * Returns a reference to the stack frame of the caller of this frame,
     * that is a frame before this frame in coroutine call stack.
     * The result is `null` for the first frame of coroutine.
     */
    public val callerFrame: CoroutineStackFrame?

    /**
     * Returns stack trace element that correspond to this stack frame.
     * The result is `null` if the stack trace element is not available for this frame.
     * In this case, the debugger represents this stack frame using the
     * result of [toString] function.
     */
    public fun getStackTraceElement(): StackTraceElement?
}