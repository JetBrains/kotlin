/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import org.jetbrains.kotlin.util.Logger

internal class CliLoggerAdapter(indentSize: Int) : Logger {
    private val indent = " ".repeat(indentSize)

    override fun log(message: String) = printlnIndented(message)
    override fun warning(message: String) = printlnIndented("Warning: $message")

    override fun error(message: String) = fatal(message)
    override fun fatal(message: String): Nothing {
        printlnIndented("Error: $message\n")
        error(message)
    }

    private fun printlnIndented(text: String) =
        if (indent.isEmpty()) println(text)
        else text.split('\n').forEach { println(indent + it) }
}
