/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.gradle.utils

import java.io.IOException

private val LINE_SEPARATOR = System.getProperty("line.separator")

internal class Printer(
    private val out: Appendable,
    private val indentUnit: String = "  ",
    private var indent: String = ""
) {
    private fun append(s: String) {
        try {
            out.append(s)
        } catch (e: IOException) { // Do nothing
        }
    }

    fun println(vararg strings: String) {
        this.print(*strings)
        printLineSeparator()
    }

    private fun printLineSeparator() {
        append(LINE_SEPARATOR)
    }

    fun print(vararg strings: String) {
        if (strings.isNotEmpty()) {
            this.printIndent()
        }
        this.printWithNoIndent(*strings)
    }

    private fun printIndent() {
        append(indent)
    }

    private fun printWithNoIndent(vararg strings: String) {
        for (s in strings) {
            append(s)
        }
    }

    fun pushIndent() {
        indent += indentUnit
    }

    fun popIndent() {
        check(indent.length >= indentUnit.length) { "No indentation to pop" }
        indent = indent.substring(indentUnit.length)
    }

    inline fun <T> withIndent(headLine: String? = null, fn: () -> T): T {
        if (headLine != null) {
            this.println(headLine)
        }
        pushIndent()

        return try {
            fn()
        } finally {
            popIndent()
        }
    }
}