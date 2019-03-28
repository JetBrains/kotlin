/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.util

import kotlin.system.measureTimeMillis
import org.jetbrains.kotlin.konan.file.*
import java.lang.StringBuilder

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove this function:
fun <T> printMillisec(message: String, body: () -> T): T {
    var result: T? = null
    val msec = measureTimeMillis{
        result = body()
    }
    println("$message: $msec msec")
    return result!!
}

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove this function:
fun profile(message: String, body: () -> Unit) = profileIf(
    System.getProperty("konan.profile")?.equals("true") ?: false,
    message, body)

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove this function:
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