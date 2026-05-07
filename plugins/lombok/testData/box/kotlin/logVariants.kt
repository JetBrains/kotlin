// ISSUE: KT-81622
// WITH_SLF4J
// FULL_JDK
// FIR_DUMP

import lombok.extern.slf4j.Slf4j

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

fun box(): String {
    val variants = listOf(
        Slf4jExample(),
        Slf4jExampleWithTopic(),
    )

    variants.forEach { it.test() }
    return "OK"
}
