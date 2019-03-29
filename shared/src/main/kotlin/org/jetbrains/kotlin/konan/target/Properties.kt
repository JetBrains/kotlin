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

package org.jetbrains.kotlin.konan.properties

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove the whole file!

import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.util.parseSpaceSeparatedArgs

typealias Properties = java.util.Properties

fun File.loadProperties(): Properties {
    val properties = java.util.Properties()
    this.bufferedReader().use { reader ->
        properties.load(reader)
    }
    return properties
}

fun loadProperties(path: String): Properties = File(path).loadProperties()

fun File.saveProperties(properties: Properties) {
    this.outputStream().use { 
        properties.store(it, null) 
    }
}

fun Properties.saveToFile(file: File) = file.saveProperties(this)

fun Properties.propertyString(key: String, suffix: String? = null): String? = getProperty(key.suffix(suffix)) ?: this.getProperty(key)

/**
 * TODO: this method working with suffixes should be replaced with
 * functionality borrowed from def file parser and unified for interop tool
 * and kotlin compiler.
 */
fun Properties.propertyList(key: String, suffix: String? = null, escapeInQuotes: Boolean = false): List<String> {
    val value = this.getProperty(key.suffix(suffix)) ?: this.getProperty(key)
    if (value?.isBlank() == true) return emptyList()
    return if (escapeInQuotes) value?.let { parseSpaceSeparatedArgs(it) } ?: emptyList()
        else value?.split(Regex("\\s+")) ?: emptyList()
}

fun Properties.hasProperty(key: String, suffix: String? = null): Boolean
    = this.getProperty(key.suffix(suffix)) != null

fun String.suffix(suf: String?): String =
    if (suf == null) this
    else "${this}.$suf"


