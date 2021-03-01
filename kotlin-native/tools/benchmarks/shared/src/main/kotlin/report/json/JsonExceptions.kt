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

class JsonInvalidValueInStrictModeException(value: Any, valueDescription: String) : Exception(
        "$value is not a valid $valueDescription as per JSON spec.\n" +
                "You can disable strict mode to serialize such values"
) {
    constructor(floatValue: Float) : this(floatValue, "float")
    constructor(doubleValue: Double) : this(doubleValue, "double")
}

class JsonUnknownKeyException(key: String) : Exception(
        "Strict JSON encountered unknown key: $key\n" +
                "You can disable strict mode to skip unknown keys"
)

class JsonParsingException(position: Int, message: String) : Exception("Invalid JSON at $position: $message")

class JsonElementTypeMismatchException(key: String, expected: String) : Exception("Element $key is not a $expected")
