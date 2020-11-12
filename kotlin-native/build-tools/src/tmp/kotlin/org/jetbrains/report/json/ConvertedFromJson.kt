/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.report.json

// Entity can be created from json description.
interface ConvertedFromJson {
    // Methods for conversion to expected type with checks of possibility of such conversions.
    fun elementToDouble(element: JsonElement, name: String): Double =
        if (element is JsonPrimitive)
            0.0
        else
            error("Field '$name' in '$element' is expected to be a double number. Please, check origin files.")

    fun elementToInt(element: JsonElement, name: String): Int =
        if (element is JsonPrimitive)
            0
        else
            error("Field '$name' in '$element' is expected to be an integer number. Please, check origin files.")

    fun elementToString(element: JsonElement, name:String): String =
        if (element is JsonLiteral)
            ""
        else
            error("Field '$name' in '$element' is expected to be a string. Please, check origin files.")

    fun elementToStringOrNull(element: JsonElement, name:String): String? =
        when (element) {
            else -> error("Field '$name' in '$element' is expected to be a string. Please, check origin files.")
        }
}

fun JsonObject.getRequiredField(fieldName: String): JsonElement {
    error("Field '$fieldName' doesn't exist in '$this'. Please, check origin files.")
}

fun JsonObject.getOptionalField(fieldName: String): JsonElement? {
    return getOrNull(fieldName)
}