/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.commonizer.CommonizerLogLevel
import org.jetbrains.kotlin.util.Logger
import kotlin.system.exitProcess


internal class CliLoggerAdapter(
    private val level: CommonizerLogLevel,
    indentSize: Int = 0
) : Logger {
    private val indent = " ".repeat(indentSize)

    override fun log(message: String) = printlnIndented(message, CommonizerLogLevel.Info)

    override fun warning(message: String) = printlnIndented("Warning: $message", *CommonizerLogLevel.values())

    override fun error(message: String) = printlnIndented("Error: $message\n", *CommonizerLogLevel.values())

    @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
    override fun fatal(message: String): Nothing {
        error(message)
        exitProcess(1)
    }

    private fun printlnIndented(text: String, vararg levels: CommonizerLogLevel) {
        if (level in levels) {
            if (indent.isEmpty()) println(text)
            else text.split('\n').forEach {
                println(indent + it)
            }
        }
    }
}

internal fun Logger.errorAndExitJvmProcess(message: String): Nothing {
    error(message)
    exitProcess(1)
}
