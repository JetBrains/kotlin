package org.jetbrains.dokka.utilities

interface DokkaLogger {
    var warningsCount: Int
    var errorsCount: Int
    fun debug(message: String)
    fun info(message: String)
    fun progress(message: String)
    fun warn(message: String)
    fun error(message: String)
}

fun DokkaLogger.report() {
    if (warningsCount > 0 || errorsCount > 0) {
        info(
            "Generation completed with $warningsCount warning" +
                    (if (warningsCount == 1) "" else "s") +
                    " and $errorsCount error" +
                    if (errorsCount == 1) "" else "s"
        )
    } else {
        info("Generation completed successfully")
    }
}

enum class LoggingLevel(val index: Int) {
    DEBUG(0), PROGRESS(1), INFO(2), WARN(3), ERROR(4);
}

/**
 * Used to decouple the transport layer from logger and make it convenient for testing
 */
fun interface MessageEmitter : (String) -> Unit {
    companion object {
        val consoleEmitter: MessageEmitter = MessageEmitter { message -> println(message) }
    }
}

class DokkaConsoleLogger(
    val minLevel: LoggingLevel = LoggingLevel.DEBUG,
    private val emitter: MessageEmitter = MessageEmitter.consoleEmitter
) : DokkaLogger {
    override var warningsCount: Int = 0
    override var errorsCount: Int = 0

    override fun debug(message: String) {
        if (shouldBeDisplayed(LoggingLevel.DEBUG)) emitter(message)
    }

    override fun progress(message: String) {
        if (shouldBeDisplayed(LoggingLevel.PROGRESS)) emitter("PROGRESS: $message")
    }

    override fun info(message: String) {
        if (shouldBeDisplayed(LoggingLevel.INFO)) emitter(message)
    }

    override fun warn(message: String) {
        if (shouldBeDisplayed(LoggingLevel.WARN)) {
            emitter("WARN: $message")
        }
        warningsCount++
    }

    override fun error(message: String) {
        if (shouldBeDisplayed(LoggingLevel.ERROR)) {
            emitter("ERROR: $message")
        }
        errorsCount++
    }

    private fun shouldBeDisplayed(messageLevel: LoggingLevel): Boolean = messageLevel.index >= minLevel.index
}
