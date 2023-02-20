/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.build.report.statistic.LoggerAdapter

class GradleLoggerAdapter(private val log: Logger) : LoggerAdapter {
    override fun debug(message: String) {
        log.debug(message)
    }

    override fun info(message: String) {
        log.info(message)
    }

    override fun warn(message: String) {
        log.warn(message)
    }

    override fun error(message: String, exception: Throwable?) {
        exception?.let { log.error(message, exception) } ?: log.error(message)
    }

    override fun lifecycle(message: String) {
        log.lifecycle(message)
    }
}