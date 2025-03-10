/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.slf4j.Logger

internal class SL4JKotlinLogger(
    private val log: Logger,
    private val prefix: String? = null,
    private val addLevelAsPrefix: Boolean = false,
) : KotlinLogger {
    private fun transformMessage(msg: String): String {
        if (prefix.isNullOrBlank()) return msg
        return prefix + msg
    }

    override fun debug(msg: String) {
        val payload = if (addLevelAsPrefix) "v: $msg" else msg
        log.debug(transformMessage(payload))
    }

    override fun lifecycle(msg: String) {
        log.info(transformMessage(msg))
    }

    override fun error(msg: String, throwable: Throwable?) {
        val payload = if (addLevelAsPrefix) "e: $msg" else msg
        log.error(transformMessage(payload), throwable)
    }

    override fun info(msg: String) {
        val payload = if (addLevelAsPrefix) "i: $msg" else msg
        log.info(transformMessage(payload))
    }

    override fun warn(msg: String, throwable: Throwable?) {
        val payload = if (addLevelAsPrefix) "w: $msg" else msg
        log.warn(transformMessage(payload), throwable)
    }

    override val isDebugEnabled: Boolean
        get() = log.isDebugEnabled
}