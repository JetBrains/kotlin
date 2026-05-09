// ISSUE: KT-81622
// WITH_ADVANCED_LOGGERS
// FULL_JDK
// FIR_DUMP

import lombok.extern.slf4j.Slf4j
import lombok.extern.log4j.Log4j
import lombok.extern.apachecommons.CommonsLog
import lombok.extern.flogger.Flogger

abstract class AbstractExample {
    fun getTestMessage(): String {
        return "Test ${this::class.simpleName}"
    }

    abstract fun test()
}

@Slf4j
class Slf4jExample : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@Slf4j(topic = "topic")
class Slf4jExampleWithTopic : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@Log4j
class Log4jExample : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@Log4j(topic = "topic")
class Log4jExampleWithTopic : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@CommonsLog
class CommonsLogExample : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@CommonsLog(topic = "topic")
class CommonsLogExampleWithTopic : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@Flogger
class FloggerExample : AbstractExample() {
    override fun test() {
        log.atInfo().log(getTestMessage())
    }
}

fun box(): String {
    val variants = listOf(
        Slf4jExample(),
        Slf4jExampleWithTopic(),
        Log4jExample(),
        Log4jExampleWithTopic(),
        CommonsLogExample(),
        CommonsLogExampleWithTopic(),
        FloggerExample(),
    )

    variants.forEach { it.test() }
    return "OK"
}
