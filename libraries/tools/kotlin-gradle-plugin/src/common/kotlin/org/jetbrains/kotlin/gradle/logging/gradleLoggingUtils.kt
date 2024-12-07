/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel

internal fun Logger.kotlinInfo(message: String) {
    this.info("[KOTLIN] $message")
}

internal fun Logger.kotlinDebug(message: String) {
    this.debug("[KOTLIN] $message")
}

internal fun Logger.kotlinWarn(message: String) {
    this.warn("[KOTLIN] $message")
}

internal inline fun Logger.kotlinDebug(message: () -> String) {
    if (isDebugEnabled) {
        kotlinDebug(message())
    }
}

internal val KotlinCompilerArgumentsLogLevel.gradleLogLevel: LogLevel
    get() = when(this) {
        KotlinCompilerArgumentsLogLevel.ERROR -> LogLevel.ERROR
        KotlinCompilerArgumentsLogLevel.WARNING -> LogLevel.WARN
        KotlinCompilerArgumentsLogLevel.INFO -> LogLevel.INFO
        KotlinCompilerArgumentsLogLevel.DEBUG -> LogLevel.DEBUG
    }

internal inline fun KotlinLogger.kotlinError(message: () -> String) {
    error("[KOTLIN] ${message()}")
}

internal inline fun KotlinLogger.kotlinWarn(message: () -> String) {
    warn("[KOTLIN] ${message()}")
}

internal inline fun KotlinLogger.kotlinInfo(message: () -> String) {
    info("[KOTLIN] ${message()}")
}

internal inline fun KotlinLogger.kotlinDebug(message: () -> String) {
    if (isDebugEnabled) {
        debug("[KOTLIN] ${message()}")
    }
}

internal fun KotlinLogger.logCompilerArgumentsMessage(
    logLevel: KotlinCompilerArgumentsLogLevel,
    message: () -> String
) {
    when (logLevel) {
        KotlinCompilerArgumentsLogLevel.ERROR -> kotlinError(message)
        KotlinCompilerArgumentsLogLevel.WARNING -> kotlinWarn(message)
        KotlinCompilerArgumentsLogLevel.INFO -> kotlinInfo(message)
        KotlinCompilerArgumentsLogLevel.DEBUG -> kotlinDebug(message)
    }
}
