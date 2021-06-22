package basic

import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.jetbrains.dokka.utilities.MessageEmitter
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggerTest {
    class AccumulatingEmitter : MessageEmitter {
        val messages: MutableList<String> = mutableListOf()
        override fun invoke(message: String) {
            messages.add(message)
        }
    }

    @Test
    fun `should display info messages if logging is info`(){
        val emitter = AccumulatingEmitter()
        val logger = DokkaConsoleLogger(LoggingLevel.INFO, emitter)

        logger.debug("Debug!")
        logger.info("Info!")

        assertTrue(emitter.messages.size > 0)
        assertTrue(emitter.messages.any { it == "Info!" })
        assertFalse(emitter.messages.any { it == "Debug!" })
    }

    @Test
    fun `should not display info messages if logging is warn`(){
        val emitter = AccumulatingEmitter()
        val logger = DokkaConsoleLogger(LoggingLevel.WARN, emitter)

        logger.warn("Warning!")
        logger.info("Info!")


        assertTrue(emitter.messages.size > 0)
        assertFalse(emitter.messages.any { it.contains("Info!") })
        assertTrue(emitter.messages.any { it.contains("Warning!") })
    }
}