/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.testApi.logger

import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.*

/*
 * Even in tests it be used in a concurrent environment, so needs to be thread safe
 */
public class TestLogger(private val logger: DokkaLogger) : DokkaLogger {
    override var warningsCount: Int by logger::warningsCount
    override var errorsCount: Int by logger::errorsCount

    private var _debugMessages = synchronizedMutableListOf<String>()
    public val debugMessages: List<String> get() = _debugMessages.toList()

    private var _infoMessages = synchronizedMutableListOf<String>()
    public val infoMessages: List<String> get() = _infoMessages.toList()

    private var _progressMessages = synchronizedMutableListOf<String>()
    public val progressMessages: List<String> get() = _progressMessages.toList()

    private var _warnMessages = synchronizedMutableListOf<String>()
    public val warnMessages: List<String> get() = _warnMessages.toList()

    private var _errorMessages = synchronizedMutableListOf<String>()
    public val errorMessages: List<String> get() = _errorMessages.toList()

    override fun debug(message: String) {
        _debugMessages.add(message)
        logger.debug(message)
    }

    override fun info(message: String) {
        _infoMessages.add(message)
        logger.info(message)
    }

    override fun progress(message: String) {
        _progressMessages.add(message)
        logger.progress(message)
    }

    override fun warn(message: String) {
        _warnMessages.add(message)
        logger.warn(message)
    }

    override fun error(message: String) {
        _errorMessages.add(message)
        logger.error(message)
    }

    private fun <T> synchronizedMutableListOf(): MutableList<T> = Collections.synchronizedList(mutableListOf())
}
