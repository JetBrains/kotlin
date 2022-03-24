/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import org.gradle.internal.serialize.PlaceholderException
import java.io.PrintWriter

/**
 * Class to be shown in default Gradle tests console reporter.
 *
 * Example console output:
 * ```
 *  sample.SampleTests.testMe FAILED
 *      AssertionError at mpplib2/src/commonTest/kotlin/sample/SampleTests.kt:9
 * ```
 */
class KotlinTestFailure(
    className: String,
    message: String?,
    private val stackTraceString: String?,
    private val stackTrace: List<StackTraceElement>? = null,
    val expected: String? = null,
    val actual: String? = null
) : PlaceholderException(
    className,
    message,
    null,
    null,
    null,
    null
) {
    override fun getStackTrace(): Array<StackTraceElement> {
        return stackTrace?.toTypedArray() ?: arrayOf()
    }

    override fun printStackTrace(s: PrintWriter?) {
        setStackTrace(getStackTrace())
        super.printStackTrace(s)
    }

    override fun fillInStackTrace(): Throwable {
        return this
    }

    override fun toString(): String {
        if (getStackTrace().isNotEmpty()) {
            return message ?: "Test failed"
        }

        return if (stackTraceString != null) {
            if (message != null && message!! !in stackTraceString) message + "\n" + stackTraceString
            else stackTraceString
        } else message ?: "Test failed"
    }
}