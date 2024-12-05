/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*

class ContextParameters {

    @Sample
    fun useContext() {
        abstract class Logger { abstract fun log(message: String) }
        class ConsoleLogger : Logger() { override fun log(message: String) = println(message) }

        context(logger: Logger) fun doSomething() {
            logger.log("hello")
            // do something
            logger.log("bye")
        }

        fun example() {
            context(ConsoleLogger()) {
                doSomething()
            }
        }
    }

    @Sample
    fun fromContextContextParameter() {
        abstract class Logger { abstract fun log(message: String) }
        class ConsoleLogger : Logger() { override fun log(message: String) = println(message) }

        fun <A> withConsoleLogger(block: context(Logger) () -> A): A =
            context(ConsoleLogger()) { block() }

        withConsoleLogger {
            fromContext<Logger>().log("start")
            println("work")
            fromContext<Logger>().log("end")
        }
    }

    @Sample
    fun fromContextReceiver() {
        abstract class Logger { abstract fun log(message: String) }
        class ConsoleLogger : Logger() { override fun log(message: String) = println(message) }

        fun <A> withConsoleLogger(block: Logger.() -> A): A =
            with(ConsoleLogger()) { block() }

        withConsoleLogger {
            fromContext<Logger>().log("start")
            println("work")
            fromContext<Logger>().log("end")
        }
    }

}

