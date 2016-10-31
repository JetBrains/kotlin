package org.jetbrains.dokka

interface DokkaLogger {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String)
}

object DokkaConsoleLogger : DokkaLogger {
    var warningCount: Int = 0

    override fun info(message: String) = println(message)
    override fun warn(message: String) {
        println("WARN: $message")
        warningCount++
    }

    override fun error(message: String) = println("ERROR: $message")

    fun report() {
        if (warningCount > 0) {
            println("generation completed with $warningCount warnings")
        } else {
            println("generation completed successfully")
        }
    }
}
