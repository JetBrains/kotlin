/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

internal class TeamCityMessageStackTraceProcessor {
    private var firstLine: String? = null

    fun process(text: String, action: (String, LogType) -> Unit): Boolean {
        return if (text.trim().startsWith("at ")) {
            firstLine?.let {
                action(it, LogType.ERROR)
            }
            firstLine = null
            action(text, LogType.ERROR)
            true
        } else {
            firstLine = text
            false
        }
    }
}