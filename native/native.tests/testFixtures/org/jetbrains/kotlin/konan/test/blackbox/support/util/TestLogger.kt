/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import java.util.logging.LogManager
import java.util.logging.Logger

/**
 * Helps to log textual messages directly from JUnit test process to Gradle console.
 *
 * Note: Need to call [initialize] before the first [log] call.
 */
internal object TestLogger {
    private const val CONFIG_FILE = "/native-tests-logging.properties"

    private val mutex = Any()
    private var wrappedLogger: Logger? = null

    fun initialize() {
        synchronized(mutex) {
            check(wrappedLogger == null) { "An attempt to re-initialize ${TestLogger::class.java}" }

            val logManager: LogManager = LogManager.getLogManager()
            TestLogger::class.java.getResourceAsStream(CONFIG_FILE)?.let(logManager::readConfiguration)
                ?: error("Test configuration file $CONFIG_FILE not found at the classpath")

            wrappedLogger = Logger.getLogger(TestLogger::class.java.name)
        }
    }

    fun log(message: String) {
        wrappedLogger?.info(message) ?: error("${TestLogger::class.java} has not been initialized")
    }
}
