/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import java.io.PrintStream
import java.io.PrintWriter

enum class TestsExceptionType(val postfix: String) {
    COMPILETIME_ERROR("compiletime");
}

sealed class TestsError(val original: Throwable, val type: TestsExceptionType) : Error() {
    override fun toString(): String = original.toString()
    override fun getStackTrace(): Array<out StackTraceElement> = original.stackTrace
    override fun initCause(cause: Throwable?): Throwable = original.initCause(cause)
    override val cause: Throwable? get() = original.cause

    // This function is called in the constructor of Throwable, where original is not yet initialized
    override fun fillInStackTrace(): Throwable? = @Suppress("UNNECESSARY_SAFE_CALL") original?.fillInStackTrace()

    override fun setStackTrace(stackTrace: Array<out StackTraceElement>?) {
        original.stackTrace = stackTrace
    }

    override fun printStackTrace() = original.printStackTrace()
    override fun printStackTrace(s: PrintStream?) = original.printStackTrace(s)
    override fun printStackTrace(s: PrintWriter?) = original.printStackTrace(s)
}

class TestsCompiletimeError(original: Throwable) : TestsError(original, TestsExceptionType.COMPILETIME_ERROR)