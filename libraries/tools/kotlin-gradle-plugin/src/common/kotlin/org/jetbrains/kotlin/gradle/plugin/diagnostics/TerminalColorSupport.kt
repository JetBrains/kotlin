/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.konan.target.HostManager

internal object TerminalColorSupport {
    /**
     * Enum representing various types of terminal environments.
     *
     * This enum class is used to differentiate between terminal environments
     * typically encountered across different operating systems.
     *
     * - `WINDOWS_CMD`: Represents the Windows Command Prompt environment.
     * - `WINDOWS_POWERSHELL`: Represents the Windows PowerShell environment.
     * - `UNIX_LIKE`: Represents Unix-like terminal environments including Linux, macOS, and other Unix-based systems.
     * - `UNKNOWN`: Represents an unrecognized or unsupported terminal environment.
     */
    private enum class TerminalType {
        WINDOWS_CMD,
        WINDOWS_POWERSHELL,
        UNIX_LIKE,
        UNKNOWN
    }

    private fun detectTerminalType() = when {
        HostManager.hostIsMingw -> {
            when {
                System.getenv("PROMPT") != null -> TerminalType.WINDOWS_CMD
                System.getenv("PSModulePath") != null -> TerminalType.WINDOWS_POWERSHELL
                else -> TerminalType.UNKNOWN
            }
        }
        HostManager.hostIsLinux || HostManager.hostIsMac -> TerminalType.UNIX_LIKE
        else -> TerminalType.UNKNOWN
    }

    // Single source of truth for color support
    private val isColorSupported = when (detectTerminalType()) {
        TerminalType.WINDOWS_CMD -> false
        TerminalType.WINDOWS_POWERSHELL -> true
        TerminalType.UNIX_LIKE -> true
        TerminalType.UNKNOWN -> false
    }

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
            operator fun invoke(text: String): String =
                if (isColorSupported) "$code$text$RESET" else text

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