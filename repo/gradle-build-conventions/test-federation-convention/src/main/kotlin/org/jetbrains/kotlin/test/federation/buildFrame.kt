/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.federation

fun buildFrame(vararg lines: String): String {
    val title = "TEST FEDERATION"
    val length = lines.maxOf { it.length } + 2
    val borderLength = if ((length - title.length) % 2 == 0) length else length + 1
    val topBorderLength = (borderLength - title.length) / 2
    val topBorder = "┌" + "─".repeat(topBorderLength) + title + "─".repeat(topBorderLength) + "┐"
    val bottomBorder = "└" + "─".repeat(borderLength) + "┘"
    val formattedLines = lines.joinToString("\n") { "│ ${it.padEnd(borderLength - 2)} │" }
    return topBorder + "\n" + formattedLines + "\n" + bottomBorder
}

