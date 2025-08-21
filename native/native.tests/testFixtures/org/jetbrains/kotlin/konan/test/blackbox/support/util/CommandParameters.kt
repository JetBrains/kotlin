/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData

internal class CommandParameters(
    private val commandName: String,
    private val command: List<String>,
) : LoggedData() {
    override fun computeText(): String = buildString {
        appendLine(commandName)
        command.forEach {
            appendLine("- $it")
        }
    }
}