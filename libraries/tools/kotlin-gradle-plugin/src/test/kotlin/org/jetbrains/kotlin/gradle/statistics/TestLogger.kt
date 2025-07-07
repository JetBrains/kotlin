/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.statistics

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.slf4j.Marker

class TestLogger(val logLevel: LogLevel) : Logger {
    val log = StringBuilder()
    override fun isLifecycleEnabled(): Boolean = true

    override fun debug(message: String?, vararg objects: Any?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(message?.format(*objects))
    }

    override fun lifecycle(message: String?) {
        if (logLevel.ordinal > LogLevel.LIFECYCLE.ordinal) return
        log.appendLine(message)
    }

    override fun lifecycle(message: String?, vararg objects: Any?) {
        if (logLevel.ordinal > LogLevel.LIFECYCLE.ordinal) return
        log.appendLine(message?.format(*objects))
    }

    override fun lifecycle(message: String?, throwable: Throwable?) {
        log.appendLine(message)
        log.appendLine(throwable?.stackTraceToString())
    }

    override fun isQuietEnabled(): Boolean = logLevel.ordinal <= LogLevel.QUIET.ordinal

    override fun quiet(message: String?) {
        if (logLevel.ordinal > LogLevel.QUIET.ordinal) return
        log.appendLine(message)
    }

    override fun quiet(message: String?, vararg objects: Any?) {
        if (logLevel.ordinal > LogLevel.QUIET.ordinal) return
        log.appendLine(message?.format(*objects))
    }

    override fun info(message: String?, vararg objects: Any?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(message?.format(*objects))
    }

    override fun quiet(message: String?, throwable: Throwable?) {
        if (logLevel.ordinal > LogLevel.QUIET.ordinal) return
        log.appendLine(message)
        log.appendLine(throwable?.stackTraceToString())
    }

    override fun isEnabled(level: LogLevel?): Boolean = true

    override fun log(level: LogLevel?, message: String?) {
        log.appendLine(message)
    }

    override fun log(level: LogLevel?, message: String?, vararg objects: Any?) {
        if (level == null || logLevel.ordinal > level.ordinal) return
        log.appendLine(message?.format(*objects))
    }

    override fun log(level: LogLevel?, message: String?, throwable: Throwable?) {
        if (level == null || logLevel.ordinal > level.ordinal) return
        log.appendLine(message)
        log.appendLine(throwable?.stackTraceToString())
    }

    override fun getName(): String = "TestLogger"

    override fun isTraceEnabled(): Boolean = false

    override fun trace(msg: String?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun trace(format: String?, arg: Any?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun trace(msg: String?, t: Throwable?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun isTraceEnabled(marker: Marker?): Boolean = false

    override fun trace(marker: Marker?, msg: String?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        // Trace level not supported in Gradle LogLevel
    }

    override fun isDebugEnabled(): Boolean = logLevel.ordinal <= LogLevel.DEBUG.ordinal

    override fun debug(msg: String?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(msg)
    }

    override fun debug(format: String?, arg: Any?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(format?.format(arg))
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(format?.format(arg1, arg2))
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(msg)
        log.appendLine(t?.stackTraceToString())
    }

    override fun isDebugEnabled(marker: Marker?): Boolean = logLevel.ordinal <= LogLevel.DEBUG.ordinal

    override fun debug(marker: Marker?, msg: String?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(msg)
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(format?.format(arg))
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(format?.format(arg1, arg2))
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(format?.format(*arguments))
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        if (logLevel.ordinal > LogLevel.DEBUG.ordinal) return
        log.appendLine(msg)
        log.appendLine(t?.stackTraceToString())
    }

    override fun isInfoEnabled(): Boolean = logLevel.ordinal <= LogLevel.INFO.ordinal

    override fun info(msg: String?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(msg)
    }

    override fun info(format: String?, arg: Any?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(format?.format(arg))
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(format?.format(arg1, arg2))
    }

    override fun info(msg: String?, t: Throwable?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(msg)
        log.appendLine(t?.stackTraceToString())
    }

    override fun isInfoEnabled(marker: Marker?): Boolean = logLevel.ordinal <= LogLevel.INFO.ordinal

    override fun info(marker: Marker?, msg: String?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(msg)
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(format?.format(arg))
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(format?.format(arg1, arg2))
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(format?.format(*arguments))
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        if (logLevel.ordinal > LogLevel.INFO.ordinal) return
        log.appendLine(msg)
        log.appendLine(t?.stackTraceToString())
    }

    override fun isWarnEnabled(): Boolean = logLevel.ordinal <= LogLevel.WARN.ordinal

    override fun warn(msg: String?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(msg)
    }

    override fun warn(format: String?, arg: Any?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(format?.format(arg))
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(format?.format(*arguments))
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(format?.format(arg1, arg2))
    }

    override fun warn(msg: String?, t: Throwable?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(msg)
        log.appendLine(t?.stackTraceToString())
    }

    override fun isWarnEnabled(marker: Marker?): Boolean = logLevel.ordinal <= LogLevel.WARN.ordinal

    override fun warn(marker: Marker?, msg: String?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(msg)
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(format?.format(arg))
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(format?.format(arg1, arg2))
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(format?.format(*arguments))
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        if (logLevel.ordinal > LogLevel.WARN.ordinal) return
        log.appendLine(msg)
        log.appendLine(t?.stackTraceToString())
    }

    override fun isErrorEnabled(): Boolean = logLevel.ordinal <= LogLevel.ERROR.ordinal

    override fun error(msg: String?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(msg)
    }

    override fun error(format: String?, arg: Any?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(format?.format(arg))
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(format?.format(arg1, arg2))
    }

    override fun error(format: String?, vararg arguments: Any?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(format?.format(*arguments))
    }

    override fun error(msg: String?, t: Throwable?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(msg)
        log.appendLine(t?.stackTraceToString())
    }

    override fun isErrorEnabled(marker: Marker?): Boolean = logLevel.ordinal <= LogLevel.ERROR.ordinal

    override fun error(marker: Marker?, msg: String?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(msg)
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(format?.format(arg))
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(format?.format(arg1, arg2))
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(format?.format(*arguments))
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        if (logLevel.ordinal > LogLevel.ERROR.ordinal) return
        log.appendLine(msg)
        log.appendLine(t?.stackTraceToString())
    }
}
