/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import java.io.File
import java.io.OutputStreamWriter
import java.lang.Appendable

/**
 * This class is a very rudimentary json serializer. It is not fully featured, and does not
 * properly handle escaping strings, among other things. This is being used because the actual
 * needs for json serialization we have are extremely minimal, and I don't want to introduce a
 * library dependency unnecessarily, however if we start serializing more objects into JSON we
 * should probably go down that path. Please use this class with caution.
 */
class JsonBuilder(private val sb: Appendable, private val indent: Int = 0) {
    var hasEntry = false

    private val nonWordCharRegex = Regex("\\W")

    private fun entryLiteral(key: String, value: String) {
        with(sb) {
            if (hasEntry) {
                appendLine(",")
            }
            append(" ".repeat(indent))
            append("\"${key.replace(nonWordCharRegex, "")}\"")
            append(": ")
            append(value)
        }
        hasEntry = true
    }
    fun entry(key: String, value: Int) = entryLiteral(key, "$value")

    fun entry(key: String, fn: JsonBuilder.() -> Unit) = entryLiteral(
        key,
        buildString { JsonBuilder(this, indent + 1).with(fn) }
    )

    fun with(fn: JsonBuilder.() -> Unit) {
        with(sb) {
            appendLine("{")
            fn()
            if (hasEntry) appendLine()
            append("}")
        }
    }
}

fun Appendable.appendJson(fn: JsonBuilder.() -> Unit) {
    JsonBuilder(this, 1).with(fn)
}

class CsvBuilder(private val writer: Appendable) {
    fun row(fn: CsvBuilder.() -> Unit): Unit = with(writer) {
        fn()
        appendLine()
    }
    fun col(value: String): Unit = with(writer) {
        require(!value.contains(',')) { "Illegal character ',' found: $value" }
        append(value)
        append(",")
    }
    fun col(value: Int): Unit = with(writer) {
        append("$value")
        append(",")
    }
    fun col(value: Boolean): Unit = with(writer) {
        append(if (value) "1" else "0")
        append(",")
    }
}

fun File.write(fn: OutputStreamWriter.() -> Unit) {
    if (!exists()) {
        parentFile.mkdirs()
        createNewFile()
    }
    writer().use {
        it.fn()
    }
}

fun Appendable.appendCsv(fn: CsvBuilder.() -> Unit) {
    CsvBuilder(this).fn()
}
