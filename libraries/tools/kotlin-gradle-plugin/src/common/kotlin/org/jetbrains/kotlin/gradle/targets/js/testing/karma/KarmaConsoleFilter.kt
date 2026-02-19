/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import org.jetbrains.kotlin.gradle.internal.LogType

open class KarmaConsoleProcessor(
    private val conditionType: (LogType?) -> Boolean,
    private val conditionMessage: (String) -> Boolean,
    private val processor: (String) -> String
) {
    fun process(actualType: LogType?, text: String): String {
        if (conditionType(actualType) && conditionMessage(text)) {
            return processor(text)
        }

        return text
    }
}

class KarmaConsoleRejector(
    conditionType: (LogType?) -> Boolean,
    conditionMessage: (String) -> Boolean,
) : KarmaConsoleProcessor(conditionType, conditionMessage, { _ -> "" })