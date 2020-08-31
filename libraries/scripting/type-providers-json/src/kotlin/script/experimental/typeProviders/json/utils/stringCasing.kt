/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.json.utils

internal fun String.toCamelCase(): String {
    if (isEmpty()) return this
    if (toUpperCase() == this) return toLowerCase()

    val parts = parts
    return parts.first().toLowerCase() + parts.drop(1).joinToString("") { it.toLowerCase().capitalize() }
}

internal fun String.toUpperCamelCase(): String {
    if (isEmpty()) return this
    if (toUpperCase() == this) return capitalize()

    return parts.joinToString("") { it.capitalize() }
}

private val lowerToCapitalSplit = Regex("([a-z])([A-Z])")
private val uppercaseWordSplit = Regex("([A-Z]+)([A-Z][a-z]|\$)")
private val invalidCharacters = Regex("[^0-9a-zA-Z]")

private val String.parts: List<String>
    get() {
        val simpleSplits = replace(regex = lowerToCapitalSplit, replacement = "$1 $2")
        val splitAfterUppercaseWord = simpleSplits.replace(regex = uppercaseWordSplit, replacement = "$1 $2")
        return splitAfterUppercaseWord.split(regex = invalidCharacters)
    }