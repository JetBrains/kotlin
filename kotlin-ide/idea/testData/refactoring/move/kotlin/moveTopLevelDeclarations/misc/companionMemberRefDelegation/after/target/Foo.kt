package target

import source.ILogging
import source.Klogging

class Foo {
    companion object : ILogging by Klogging()

    fun baz() {
        logger.debug { "something" }
        log("something")
    }
}