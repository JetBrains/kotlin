/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

/**
 * Class to be shown in default Gradle tests console reporter.
 *
 * Example console output:
 * ```
 *  clientTest.CommonTest.test1 FAILED
 *     org.jetbrains.kotlin.gradle.testing.KotlinTestFailure
 * ```
 */
class KotlinTestFailure(message: String?, private val stackTrace: String?) : Throwable(message) {
    override fun fillInStackTrace(): Throwable = this
    override fun toString(): String =
        if (stackTrace != null) {
            if (message != null && message !in stackTrace) message + "\n" + stackTrace
            else stackTrace
        } else message ?: "Test failed"
}