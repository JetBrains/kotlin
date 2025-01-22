/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils.processes

/**
 * The result of running an external process using [ExecHandle].
 */
internal data class ExecResult(
    /** The exit value of the process. */
    val exitValue: Int,
    private val displayName: String,
    private val failure: ExecException? = null,
) {

    /**
     * Throws [ExecException] if the process exited with a non-zero exit value.
     */
    fun assertNormalExitValue() {
        if (exitValue != 0) {
            throw ExecException("Process '$displayName' finished with non-zero exit value $exitValue")
        }
    }

    /**
     * Re-throws any failure executing this process.
     */
    fun rethrowFailure() {
        if (failure != null) throw failure
    }

    override fun toString(): String = "{exitValue=$exitValue, failure=$failure}"
}
