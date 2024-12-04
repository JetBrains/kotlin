/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.buildtools.api.KotlinLogger

class GradleLoggerAdapter(private val log: Logger) : KotlinLogger {

    override val isDebugEnabled: Boolean
        get() = log.isDebugEnabled

    override fun info(msg: String) {
        log.info(msg)
    }

    override fun debug(msg: String) {
        log.debug(msg)
    }

    override fun warn(msg: String) {
        log.warn(msg)
    }

    override fun error(msg: String, throwable: Throwable?) {
        throwable?.let { log.error(msg, throwable) } ?: log.error(msg)
    }

    override fun lifecycle(msg: String) {
        log.lifecycle(msg)
    }
}