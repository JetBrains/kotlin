/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.buildtools.api.KotlinLogger

internal class ExceptionReportingKotlinLogger : KotlinLogger {
    private var extracted: Boolean = false
    private val exceptionMessages = ArrayList<String>()

    override val isDebugEnabled: Boolean
        get() = false

    override fun error(msg: String, throwable: Throwable?) {
        if (extracted) throw IllegalStateException("Can't log after extracting messages")
        if (throwable != null) {
            exceptionMessages.add(msg)
        }
    }

    fun extractExceptionMessages(): List<String> {
        extracted = true
        return exceptionMessages.toList()
    }

    override fun warn(msg: String, throwable: Throwable?) {
        if (extracted) throw IllegalStateException("Can't log after extracting messages")
    }

    override fun info(msg: String) {
        if (extracted) throw IllegalStateException("Can't log after extracting messages")
    }

    override fun debug(msg: String) {
        if (extracted) throw IllegalStateException("Can't log after extracting messages")
    }

    override fun lifecycle(msg: String) {
        if (extracted) throw IllegalStateException("Can't log after extracting messages")
    }
}