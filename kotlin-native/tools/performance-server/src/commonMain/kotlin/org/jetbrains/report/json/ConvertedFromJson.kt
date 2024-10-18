/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.report.json

// Entity can be created from json description.
interface ConvertedFromJson {
    // Methods for conversion to expected type with checks of possibility of such conversions.
    fun elementToDouble(element: JsonElement, name: String): Double =
            if (element is JsonPrimitive)
                element.double
            else
                error("Field '$name' in '$element' is expected to be a double number. Please, check origin files.")

    fun elementToInt(element: JsonElement, name: String): Int =
            if (element is JsonPrimitive)
                element.int
            else
                error("Field '$name' in '$element' is expected to be an integer number. Please, check origin files.")

    fun elementToString(element: JsonElement, name:String): String =
            if (element is JsonLiteral)
                element.unquoted()
            else
                error("Field '$name' in '$element' is expected to be a string. Please, check origin files.")

    fun elementToStringOrNull(element: JsonElement, name:String): String? =
            when (element) {
                is JsonLiteral -> element.unquoted()
                is JsonNull -> null
                else -> error("Field '$name' in '$element' is expected to be a string. Please, check origin files.")
            }
}

fun JsonObject.getRequiredField(fieldName: String): JsonElement {
    return getOrNull(fieldName) ?: error("Field '$fieldName' doesn't exist in '$this'. Please, check origin files.")
}

fun JsonObject.getOptionalField(fieldName: String): JsonElement? {
    return getOrNull(fieldName)
}