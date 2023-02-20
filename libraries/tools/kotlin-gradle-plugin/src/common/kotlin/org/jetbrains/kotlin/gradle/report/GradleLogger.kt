/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logger

class GradleLoggerAdapter(private val log: Logger) : org.jetbrains.kotlin.util.Logger {
    override fun log(message: String) {
        log.info(message)
    }

    override fun warning(message: String) {
        log.warn(message)
    }

    override fun fatal(message: String): Nothing {
        log.error(message)
        kotlin.error(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        throwable?.let { log.error(message, throwable) } ?: log.error(message)
    }

    override fun lifecycle(message: String) {
        log.lifecycle(message)
    }
}