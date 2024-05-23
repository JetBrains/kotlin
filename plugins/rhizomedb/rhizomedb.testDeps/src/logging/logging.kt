package logging

import fleet.util.logging.BaseLogger
import fleet.util.logging.KLogger
import fleet.util.logging.KLoggerFactory
import kotlin.reflect.KClass

class TestLogger : KLoggerFactory {
    override fun logger(owner: KClass<*>): KLogger {
        return defaultLogger(owner)
    }

    override fun logger(owner: Class<*>): KLogger {
        return defaultLogger(owner.kotlin)
    }

    override fun logger(owner: Any): KLogger {
        return defaultLogger(owner.javaClass.kotlin)
    }

    override fun logger(name: String): KLogger {
        return defaultLogger(name)
    }

    private fun defaultLogger(name: String): KLogger {
        return KLogger(PrintlnLogger)
    }

    private fun defaultLogger(clazz: KClass<*>): KLogger {
        return defaultLogger(clazz.java)
    }

    private fun defaultLogger(clazz: Class<*>): KLogger {
        return KLogger(PrintlnLogger)
    }
}

object PrintlnLogger : BaseLogger {
    override val isTraceEnabled: Boolean get() = false
    override val isDebugEnabled: Boolean get() = false
    override val isInfoEnabled: Boolean get() = false
    override val isWarnEnabled: Boolean get() = true
    override val isErrorEnabled: Boolean get() = true

    override fun trace(message: Any?) {
        println("TRACE: $message")
    }

    override fun debug(message: Any?) {
        println("DEBUG: $message")
    }

    override fun info(message: Any?) {
        println("INFO: $message")
    }

    override fun warn(message: Any?) {
        println("WARN: $message")
    }

    override fun error(message: Any?) {
        println("ERROR: $message")
    }

    override fun trace(t: Throwable?, message: Any?) {
        println("TRACE: $message, $t")
    }

    override fun debug(t: Throwable?, message: Any?) {
        println("DEBUG: $message, $t")
    }

    override fun info(t: Throwable?, message: Any?) {
        println("INFO: $message, $t")
    }

    override fun warn(t: Throwable?, message: Any?) {
        println("WARN: $message, $t")
    }

    override fun error(t: Throwable?, message: Any?) {
        println("ERROR: $message, $t")
    }
}
