class Logger {
    fun log(){}
}

abstract class WithLogging {
    val logger = Logger()
}

class C {
    companion object: WithLogging()

    fun foo() {
        logger.log()
    }
}

fun main(args: Array<String>) {
    C()
}
