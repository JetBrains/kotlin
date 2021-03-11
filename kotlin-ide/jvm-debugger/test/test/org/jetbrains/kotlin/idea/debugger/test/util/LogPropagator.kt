/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.util

import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches

internal class LogPropagator(val systemLogger: (String) -> Unit) {
    private var oldLogLevel: Level? = null
    private val logger = Logger.getLogger(KotlinDebuggerCaches::class.java)
    private var appender: AppenderSkeleton? = null

    fun attach() {
        oldLogLevel = logger.level
        logger.level = Level.DEBUG

        appender = object : AppenderSkeleton() {
            override fun append(event: LoggingEvent?) {
                val message = event?.renderedMessage
                if (message != null) {
                    systemLogger(message)
                }
            }

            override fun close() {}
            override fun requiresLayout() = false
        }

        logger.addAppender(appender)
    }

    fun detach() {
        logger.removeAppender(appender)
        appender = null

        logger.level = oldLogLevel
    }
}