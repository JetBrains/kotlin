/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.DefaultLogger
import com.intellij.openapi.diagnostic.Logger

internal class NoopIntellijLoggerFactory : Logger.Factory {
    override fun getLoggerInstance(p0: String): Logger = NoopIntellijLogger
}

/**
 * Ignores all messages passed to it
 */
internal object NoopIntellijLogger : DefaultLogger(null) {
    override fun isDebugEnabled(): Boolean = false
    override fun isTraceEnabled(): Boolean = false

    override fun debug(message: String?) {}
    override fun debug(t: Throwable?) {}
    override fun debug(message: String?, t: Throwable?) {}
    override fun debug(message: String, vararg details: Any?) {}
    override fun debugValues(header: String, values: MutableCollection<*>) {}

    override fun trace(message: String?) {}
    override fun trace(t: Throwable?) {}

    override fun info(message: String?) {}
    override fun info(message: String?, t: Throwable?) {}
    override fun info(t: Throwable) {}

    override fun warn(message: String?, t: Throwable?) {}
    override fun warn(message: String?) {}
    override fun warn(t: Throwable) {}

    override fun error(message: String?, t: Throwable?, vararg details: String?) {}
    override fun error(message: String?) {}
    override fun error(message: Any?) {}
    override fun error(message: String?, vararg attachments: Attachment?) {}
    override fun error(message: String?, t: Throwable?, vararg attachments: Attachment?) {}
    override fun error(message: String?, vararg details: String?) {}
    override fun error(message: String?, t: Throwable?) {}
    override fun error(t: Throwable) {}
}
