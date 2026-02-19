/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.buildtools.api.KotlinLogger

internal class CompositeKotlinLogger(
    private val loggers: Set<KotlinLogger>,
) : KotlinLogger {
    override val isDebugEnabled: Boolean
        get() = loggers.any { it.isDebugEnabled }

    override fun error(msg: String, throwable: Throwable?) {
        loggers.forEach { it.error(msg, throwable) }
    }

    override fun warn(msg: String, throwable: Throwable?) {
        loggers.forEach { it.warn(msg, throwable) }
    }

    override fun info(msg: String) {
        loggers.forEach { it.info(msg) }
    }

    override fun debug(msg: String) {
        loggers.forEach { it.debug(msg) }
    }

    override fun lifecycle(msg: String) {
        loggers.forEach { it.lifecycle(msg) }
    }
}