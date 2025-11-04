/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

internal object TerminalColorSupport {

    /**
     * Provides ANSI escape codes for applying various styles and colors to terminal text.
     * Includes constants for commonly used styles and colors as well as extension functions
     * to easily style strings.
     *
     * The object is designed to simplify the process of formatting text for terminal output.
     * Reset codes are automatically appended after applying styles to ensure proper formatting.
     */
    internal object TerminalStyle {
        // Style definitions
        sealed class Style(private val code: String) {
            operator fun invoke(text: String): String = "$code$text$RESET"

            companion object {
                private const val RESET = "\u001B[0m"
            }
        }

        // Style objects
        object Bold : Style("\u001B[1m")
        object Italic : Style("\u001B[3m")
        object Yellow : Style("\u001B[33m")
        object Green : Style("\u001B[32m")
        object Red : Style("\u001B[31m")
        object Blue : Style("\u001B[34m")
        object LightBlue : Style("\u001B[36m")
        object Orange : Style("\u001B[38;5;214m")

        // Extension functions using style objects
        fun String.bold() = Bold(this)
        fun String.italic() = Italic(this)
        fun String.yellow() = Yellow(this)
        fun String.green() = Green(this)
        fun String.red() = Red(this)
        fun String.blue() = Blue(this)
        fun String.lightBlue() = LightBlue(this)
        fun String.orange() = Orange(this)
    }
}