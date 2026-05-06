// FULL_JDK
// FIR_DUMP

import lombok.extern.java.Log
import lombok.AccessLevel
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

val logMessages = mutableListOf<String>()

val logHandler = object : Handler() {
    override fun publish(record: LogRecord) {
        logMessages.add(record.level.toString() + ": " + record.message)
    }

    override fun flush() {}
    override fun close() {}
}

@Log(access = AccessLevel.PUBLIC)
class LogExample {
    fun test() {
        val logLocal: Logger = log
        logLocal.info("Test LogExample")
    }
}

@Log
class LogExampleWithExistingCompanion {
    companion object Companion {}

    fun test() {
        log.addHandler(logHandler)
        log.warning("Test LogExampleWithExistingCompanion")
    }
}

@Log
class LogExampleWithExistingCompanionAndLogField {
    companion object MyCompanion {
        val log = "No log"
    }

    fun test(): String {
        return log
    }
}

@Log(topic = "custom topic")
class LogExampleWithTopic {
    fun test() {
        log.addHandler(logHandler)
        log.info("Test LogExampleWithTopic")
    }
}

class LogOnCompanion {
    @Log
    companion object {
        fun test() {
            log.addHandler(logHandler)
            log.info("Check @Log on companion object")
        }
    }
}

class LogOnNestedClass {
    @Log
    class Nested {
        fun test() {
            log.addHandler(logHandler)
            log.info("Check @Log on nested class")
        }
    }
}

class LogOnInnerClass<T> {
    @Log
    inner class Inner {
        fun test() {
            log.addHandler(logHandler)
            // Companion object are prohibited inside inner classes (`NESTED_CLASS_NOT_ALLOWED`), but it somehow works
            log.info("Check @Log on inner class")
        }
    }
}

@Log
object LogOnObject {
    fun test() {
        log.addHandler(logHandler)
        log.info("Check @Log on object")
    }
}

@Log
enum class LogOnEnum {
    ExampleEntry;

    fun test() {
        log.addHandler(logHandler)
        log.info("Check @Log on enum")
    }
}

@Log
class LogWhenNonConflictingExtensionProperty {
    companion object {
        val Int.log: Int get() = this + 1

        fun test() {
            log.addHandler(logHandler)
            log.info("Check LogWhenNonConflictingExtensionProperty")
        }
    }
}

@Log
class LogWhenNonConflictingContextualProperty {
    companion object {
        context(x: Int)
        val log: Int get() = x + 1

        fun test() {
            log.addHandler(logHandler)
            log.info("Check LogWhenNonConflictingContextualProperty")
        }
    }
}

fun box(): String {
    LogExample.log.addHandler(logHandler)
    LogExample.log.info("Call from public log")
    LogExample().test()
    LogExampleWithExistingCompanion().test()
    assertEquals("No log", LogExampleWithExistingCompanionAndLogField().test())
    LogExampleWithTopic().test()
    LogOnCompanion.test()
    LogOnNestedClass.Nested().test()
    LogOnInnerClass<String>().Inner().test()
    LogOnObject.test()
    LogOnEnum.ExampleEntry.test()
    LogWhenNonConflictingExtensionProperty.test()
    LogWhenNonConflictingContextualProperty.test()

    val expected = listOf(
        "INFO: Call from public log",
        "INFO: Test LogExample",
        "WARNING: Test LogExampleWithExistingCompanion",
        "INFO: Test LogExampleWithTopic",
        "INFO: Check @Log on companion object",
        "INFO: Check @Log on nested class",
        "INFO: Check @Log on inner class",
        "INFO: Check @Log on object",
        "INFO: Check @Log on enum",
        "INFO: Check LogWhenNonConflictingExtensionProperty",
        "INFO: Check LogWhenNonConflictingContextualProperty",
    )
    assertEquals(expected, logMessages)
    return "OK"
}
