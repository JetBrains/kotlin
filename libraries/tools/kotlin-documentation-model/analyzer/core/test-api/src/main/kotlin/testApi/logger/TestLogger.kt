package org.jetbrains.dokka.testApi.logger

import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.Collections

/*
 * Even in tests it be used in a concurrent environment, so needs to be thread safe
 */
class TestLogger(private val logger: DokkaLogger) : DokkaLogger {
    override var warningsCount: Int by logger::warningsCount
    override var errorsCount: Int by logger::errorsCount

    private var _debugMessages = synchronizedMutableListOf<String>()
    val debugMessages: List<String> get() = _debugMessages.toList()

    private var _infoMessages = synchronizedMutableListOf<String>()
    val infoMessages: List<String> get() = _infoMessages.toList()

    private var _progressMessages = synchronizedMutableListOf<String>()
    val progressMessages: List<String> get() = _progressMessages.toList()

    private var _warnMessages = synchronizedMutableListOf<String>()
    val warnMessages: List<String> get() = _warnMessages.toList()

    private var _errorMessages = synchronizedMutableListOf<String>()
    val errorMessages: List<String> get() = _errorMessages.toList()

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
