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

package org.jetbrains.kotlin.js.testOld

import org.jetbrains.kotlin.js.parser.sourcemaps.JsonArray
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonBoolean
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonNode
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonNull
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonNumber
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonObject
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonString
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonTest {
    @Test
    fun complex() {
        val input = """
            {
                "a": [ 1, 2, { "x" : 3 }, [ 5 ] ],
                "b": 1,
                "c": { "foo": null, "bar": true, "baz": false },
                "d": null,
                "e": "qwe",
                "f": 1.2,
                "g": [ [], {} ]
            }
        """.trimIndent()

        val first = org.jetbrains.kotlin.js.parser.sourcemaps.parseJson(input)
        val second = org.jetbrains.kotlin.js.parser.sourcemaps.parseJson(first.toString())

        val expected = JsonObject(
                "a" to JsonArray(
                        JsonNumber(1.0),
                        JsonNumber(2.0),
                        JsonObject("x" to JsonNumber(3.0)),
                        JsonArray(JsonNumber(5.0))
                ),
                "b" to JsonNumber(1.0),
                "c" to JsonObject(
                        "foo" to JsonNull,
                        "bar" to JsonBoolean.TRUE,
                        "baz" to JsonBoolean.FALSE
                ),
                "d" to JsonNull,
                "e" to JsonString("qwe"),
                "f" to JsonNumber(1.2),
                "g" to JsonArray(JsonArray(), JsonObject())
        )

        assertJsonEquals(expected, first)
        assertJsonEquals(expected, second)
    }

    @Test
    fun numbers() {
        assertJsonEquals(JsonNumber(1.0), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("1"))
        assertJsonEquals(JsonNumber(1.2), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("1.2"))
        assertJsonEquals(JsonNumber(10.2), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("10.2"))
        assertJsonEquals(JsonNumber(10.02), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("10.02"))
        assertJsonEquals(JsonNumber(0.2), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("0.2"))
        assertJsonEquals(JsonNumber(1000.0), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("1E3"))
        assertJsonEquals(JsonNumber(0.001), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("1E-3"))
        assertJsonEquals(JsonNumber(0.0012), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("1.2e-3"))
        assertJsonEquals(JsonNumber(1200.0), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("1.2e+3"))
        assertJsonEquals(JsonNumber(-1200.0), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("-1.2e+3"))
    }

    @Test
    fun strings() {
        assertJsonEquals(JsonString("foo"), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("\"foo\""))
        assertJsonEquals(JsonString(""), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("\"\""))
        assertJsonEquals(JsonString("\r\n\t\b\u000C\u0012 Й"), org.jetbrains.kotlin.js.parser.sourcemaps.parseJson("\"\\r\\n\\t\\b\\f\\u0012 Й\""))
    }

    private fun assertJsonEquals(expected: JsonNode, actual: JsonNode) {
        assertEquals(expected, actual)
        assertEquals(expected.toString(), actual.toString())
    }
}