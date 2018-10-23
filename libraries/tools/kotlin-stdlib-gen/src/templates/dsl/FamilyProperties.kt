/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*

val Family.DocExtension.collection: String
    get() = with(DocExtensions) { family.collection }

val Family.DocExtension.element: String
    get() = with(DocExtensions) { family.element }

val Family.CodeExtension.size: String
    get() = when (family) {
        Iterables, Collections, Lists, Sets, Maps, InvariantArraysOfObjects, ArraysOfObjects, ArraysOfPrimitives -> "size"
        CharSequences, Strings -> "length"
        else -> error("size property isn't supported for $family")
    }

object DocExtensions {

    val Family.element: String
        get() = when (this) {
            Strings, CharSequences -> "character"
            Maps -> "entry"
            else -> "element"
        }

    val Family.collection: String
        get() = when (this) {
            CharSequences -> "char sequence"
            ArraysOfObjects, ArraysOfPrimitives, InvariantArraysOfObjects, ArraysOfUnsigned -> "array"
            Ranges, RangesOfPrimitives -> "range"
            Strings, Sequences, Maps, Lists, Sets -> name.singularize().decapitalize()
            else -> "collection"
        }

    val Family.mapResult: String
        get() = when (this) {
            Sequences -> "sequence"
            else -> "list"
        }

    val PrimitiveType?.zero: String
        get() = when (this) {
            null -> "`null`"
            PrimitiveType.Boolean -> "`false`"
            PrimitiveType.Char -> "null char (`\\u0000`)"
            else -> "zero"
        }

    fun textWhen(condition: Boolean, text: () -> String): String = if (condition) text() else ""

    private fun String.singularize() = removeSuffix("s")

    public fun String.pluralize() = when {
        this.endsWith("y") -> this.dropLast(1) + "ies"
        else -> this + "s"
    }

    fun String.prefixWithArticle() = (if ("aeiou".any { this.startsWith(it, ignoreCase = true) }) "an " else "a ") + this

}
