/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.properties

import org.jetbrains.kotlin.konan.file.*

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
fun Properties.propertyList(key: String, suffix: String? = null): List<String> {
    val value = this.getProperty(key.suffix(suffix)) ?: this.getProperty(key)
    if (value?.isBlank() == true) return emptyList()

    return value?.split(Regex("\\s+")) ?: emptyList()
}

fun Properties.hasProperty(key: String, suffix: String? = null): Boolean = this.getProperty(key.suffix(suffix)) != null

fun String.suffix(suf: String?): String = if (suf == null) this else "${this}.$suf"

