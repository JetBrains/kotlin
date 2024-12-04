/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal.state

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.SL4JKotlinLogger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.util.HashMap

// todo: remove when https://github.com/gradle/gradle/issues/16991 is resolved
internal object TaskLoggers {
    private val taskLoggers = HashMap<String, WeakReference<Logger>>()

    @Synchronized
    fun put(path: String, logger: Logger) {
        taskLoggers[path] = WeakReference(logger)
    }

    @Synchronized
    fun get(path: String): Logger? =
        taskLoggers[path]?.get()

    @Synchronized
    fun clear() {
        taskLoggers.clear()
    }
}

internal fun getTaskLogger(taskPath: String, prefix: String?, fallbackLoggerName: String) =
    TaskLoggers.get(taskPath)?.let { GradleKotlinLogger(it, prefix).apply { debug("Using '$taskPath' logger") } }
        ?: run {
            val logger = LoggerFactory.getLogger(fallbackLoggerName)
            val kotlinLogger = if (logger is Logger) {
                GradleKotlinLogger(logger, prefix)
            } else SL4JKotlinLogger(logger, prefix)

            kotlinLogger.apply {
                debug("Could not get logger for '$taskPath'. Falling back to sl4j logger")
            }
        }