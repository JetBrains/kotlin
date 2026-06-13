// ISSUE: KT-81622
// WITH_ADVANCED_LOGGERS
// FULL_JDK
// FIR_DUMP

import lombok.extern.slf4j.Slf4j
import lombok.extern.log4j.Log4j
import lombok.extern.apachecommons.CommonsLog
import lombok.extern.flogger.Flogger
import lombok.extern.jbosslog.JBossLog
import lombok.extern.log4j.Log4j2
import lombok.extern.slf4j.XSlf4j

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

@JBossLog
class JBossLogExample : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@JBossLog(topic = "topic")
class JBossLogExampleWithTopic : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@Log4j2
class Log4j2Example : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@Log4j2(topic = "topic")
class Log4j2ExampleWithTopic : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@XSlf4j
class XSlf4jExample : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
    }
}

@XSlf4j(topic = "topic")
class XSlf4jExampleWithTopic : AbstractExample() {
    override fun test() {
        log.info(getTestMessage())
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
        JBossLogExample(),
        JBossLogExampleWithTopic(),
        Log4j2Example(),
        Log4j2ExampleWithTopic(),
        XSlf4jExample(),
        XSlf4jExampleWithTopic(),
    )

    variants.forEach { it.test() }
    return "OK"
}
