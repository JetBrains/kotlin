/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.buildtools.api.KotlinLogger

internal class GradleKotlinLogger(private val log: Logger, private val prefix: String? = null) : KotlinLogger {
    private fun transformMessage(msg: String): String {
        if (prefix.isNullOrBlank()) return msg
        return prefix + msg
    }

    override fun debug(msg: String) {
        log.debug(transformMessage(msg))
    }

    override fun error(msg: String, throwable: Throwable?) {
        log.error(transformMessage(msg), throwable)
    }

    override fun info(msg: String) {
        log.info(transformMessage(msg))
    }

    override fun warn(msg: String) {
        log.warn(transformMessage(msg))
    }

    override fun lifecycle(msg: String) {
        log.lifecycle(transformMessage(msg))
    }

    override val isDebugEnabled: Boolean
        get() = log.isDebugEnabled
}