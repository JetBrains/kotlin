/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.build.report.statistic.LoggerAdapter

class JpsLoggerAdapter(private val log: Logger) : LoggerAdapter {
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
        log.error(message, exception)
    }

    override fun lifecycle(message: String) {
        log.info(message)
    }
}