/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*

class ContextParameters {

    @Sample
    fun implicitContextParameter() {
        interface Logger { fun log(message: String) }

        class ConsoleLogger : Logger { override fun log(message: String) = println(message) }

        fun <A> withConsoleLogger(block: context(Logger) () -> A): A =
            context(ConsoleLogger()) { block() }

        withConsoleLogger {
            implicit<Logger>().log("start")
            println("work")
            implicit<Logger>().log("end")
        }
    }

    @Sample
    fun implicitReceiver() {
        interface Logger { fun log(message: String) }

        class ConsoleLogger : Logger { override fun log(message: String) = println(message) }

        fun <A> withConsoleLogger(block: Logger.() -> A): A =
            with(ConsoleLogger()) { block() }

        withConsoleLogger {
            implicit<Logger>().log("start")
            println("work")
            implicit<Logger>().log("end")
        }
    }

}

