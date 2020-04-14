/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

internal class TeamCityMessageStackTraceProcessor {
    private var firstLine: String? = null

    fun process(text: String, firstLineAction: (String?) -> Unit) {
        firstLine = if (text.trim().startsWith("at ")) {
            // firstLineAction can have side effects
            // that's why important to call it even with null line
            firstLineAction(firstLine)
            null
        } else {
            text
        }
    }
}