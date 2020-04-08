package source

class Logger {
    fun debug(s: () -> String) {

    }
}

open class Klogging {
    val logger = Logger()
    fun log(s: String) {}
}

