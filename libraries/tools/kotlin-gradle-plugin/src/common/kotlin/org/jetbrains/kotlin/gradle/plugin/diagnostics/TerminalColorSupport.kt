/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.konan.target.HostManager

object TerminalColorSupport {
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
    enum class TerminalType {
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

    private fun supportsColor() = when (detectTerminalType()) {
        TerminalType.WINDOWS_CMD -> false // No native ANSI support
        TerminalType.WINDOWS_POWERSHELL -> true // Supports ANSI from PS 6.0+
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
    object TerminalStyle {
        // ANSI color and style constants
        private const val RESET = "\u001B[0m"
        private const val YELLOW = "\u001B[33m"
        private const val GREEN = "\u001B[32m"
        private const val BOLD = "\u001B[1m"
        private const val ITALIC = "\u001B[3m"
        private const val RED = "\u001B[31m"
        private const val BLUE = "\u001B[34m"
        private const val LIGHT_BLUE = "\u001B[36m"
        private const val ORANGE = "\u001B[38;5;214m"

        // Convenience extension functions for styling
        fun String.bold() = if (supportsColor()) "$BOLD$this$RESET" else ""
        fun String.italic() = if (supportsColor()) "$ITALIC$this$RESET" else ""
        fun String.yellow() = if (supportsColor()) "$YELLOW$this$RESET" else ""
        fun String.green() = if (supportsColor()) "$GREEN$this$RESET" else ""
        fun String.red() = if (supportsColor()) "$RED$this$RESET" else ""
        fun String.blue() = if (supportsColor()) "$BLUE$this$RESET" else ""
        fun String.lightBlue() = if (supportsColor()) "$LIGHT_BLUE$this$RESET" else ""
        fun String.orange() = if (supportsColor()) "$ORANGE$this$RESET" else ""
    }
}