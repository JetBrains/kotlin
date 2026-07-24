/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.util

fun <T> StringBuilder.appendListIfNonEmpty(data: List<T>, openBracket: CharSequence, closeBracket: CharSequence, transform: ((T) -> CharSequence)? = null) {
    if (data.isEmpty()) return
    appendList(data, openBracket, closeBracket, transform)
}

fun <T> StringBuilder.appendList(data: List<T>, openBracket: CharSequence, closeBracket: CharSequence, transform: ((T) -> CharSequence)? = null) {
    data.joinTo(this, prefix = openBracket, postfix = closeBracket, separator = ", ", transform = transform)
}

fun joinPairedText(data: List<Pair<*, String>>, openBracket: CharSequence, closeBracket: CharSequence): String =
    buildString { appendList(data, openBracket, closeBracket) { it.second } }
