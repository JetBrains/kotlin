/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

import kotlin.system.measureTimeMillis
import org.jetbrains.kotlin.konan.file.*
import java.lang.StringBuilder

fun <T> printMillisec(message: String, body: () -> T): T {
    var result: T? = null
    val msec = measureTimeMillis{
        result = body()
    }
    println("$message: $msec msec")
    return result!!
}

fun profile(message: String, body: () -> Unit) = profileIf(
    System.getProperty("konan.profile")?.equals("true") ?: false,
    message, body)

fun profileIf(condition: Boolean, message: String, body: () -> Unit) =
    if (condition) printMillisec(message, body) else body()

fun nTabs(amount: Int): String {
    return String.format("%1$-${(amount+1)*4}s", "")
}

fun String.prefixIfNot(prefix: String) =
    if (this.startsWith(prefix)) this else "$prefix$this"

fun String.prefixBaseNameIfNot(prefix: String): String {
    val file = File(this).absoluteFile
    val name = file.name
    val directory = file.parent
    return "$directory/${name.prefixIfNot(prefix)}"
}

fun String.suffixIfNot(suffix: String) =
    if (this.endsWith(suffix)) this else "$this$suffix"

fun String.removeSuffixIfPresent(suffix: String) =
    if (this.endsWith(suffix)) this.dropLast(suffix.length) else this

fun <T> Lazy<T>.getValueOrNull(): T? = if (isInitialized()) value else null

fun parseSpaceSeparatedArgs(argsString: String): List<String> {
    val parsedArgs = mutableListOf<String>()
    var inQuotes = false
    var currentCharSequence = StringBuilder()
    fun saveArg() {
        if (!currentCharSequence.isEmpty()) {
            parsedArgs.add(currentCharSequence.toString())
            currentCharSequence = StringBuilder()
        }
    }
    argsString.forEach { char ->
        if (char == '"') {
            inQuotes = !inQuotes
            // Save value which was in quotes.
            if (!inQuotes) {
                saveArg()
            }
        } else if (char == ' ' && !inQuotes) {
            // Space is separator.
            saveArg()
        } else {
            currentCharSequence.append(char)
        }
    }
    if (inQuotes) {
        error("No close-quote was found in $currentCharSequence.")
    }
    saveArg()
    return parsedArgs
}