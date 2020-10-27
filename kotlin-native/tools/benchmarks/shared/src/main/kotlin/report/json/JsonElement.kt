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

/**
 * Class representing single JSON element.
 * Can be [JsonPrimitive], [JsonArray] or [JsonObject].
 *
 * [JsonElement.toString] properly prints JSON tree as valid JSON, taking into
 * account quoted values and primitives
 */
sealed class JsonElement {

    /**
     * Convenience method to get current element as [JsonPrimitive]
     * @throws JsonElementTypeMismatchException is current element is not a [JsonPrimitive]
     */
    open val primitive: JsonPrimitive
        get() = error("JsonLiteral")

    /**
     * Convenience method to get current element as [JsonObject]
     * @throws JsonElementTypeMismatchException is current element is not a [JsonObject]
     */
    open val jsonObject: JsonObject
        get() = error("JsonObject")

    /**
     * Convenience method to get current element as [JsonArray]
     * @throws JsonElementTypeMismatchException is current element is not a [JsonArray]
     */
    open val jsonArray: JsonArray
        get() = error("JsonArray")

    /**
     * Convenience method to get current element as [JsonNull]
     * @throws JsonElementTypeMismatchException is current element is not a [JsonNull]
     */
    open val jsonNull: JsonNull
        get() = error("JsonPrimitive")

    /**
     * Checks whether current element is [JsonNull]
     */
    val isNull: Boolean
        get() = this === JsonNull

    private fun error(element: String): Nothing =
            throw JsonElementTypeMismatchException(this::class.toString(), element)
}

/**
 * Class representing JSON primitive value. Can be either [JsonLiteral] or [JsonNull].
 */
sealed class JsonPrimitive : JsonElement() {

    /**
     * Content of given element without quotes. For [JsonNull] this methods returns `"null"`
     */
    abstract val content: String

    /**
     * Content of the given element without quotes or `null` if current element is [JsonNull]
     */
    abstract val contentOrNull: String?

    @Suppress("LeakingThis")
    final override val primitive: JsonPrimitive = this

    /**
     * Returns content of current element as int
     * @throws NumberFormatException if current element is not a valid representation of number
     */
    val int: Int get() = content.toInt()

    /**
     * Returns content of current element as int or `null` if current element is not a valid representation of number
     **/
    val intOrNull: Int? get() = content.toIntOrNull()

    /**
     * Returns content of current element as long
     * @throws NumberFormatException if current element is not a valid representation of number
     */
    val long: Long get() = content.toLong()

    /**
     * Returns content of current element as long or `null` if current element is not a valid representation of number
     */
    val longOrNull: Long? get() = content.toLongOrNull()

    /**
     * Returns content of current element as double
     * @throws NumberFormatException if current element is not a valid representation of number
     */
    val double: Double get() = content.toDouble()

    /**
     * Returns content of current element as double or `null` if current element is not a valid representation of number
     */
    val doubleOrNull: Double? get() = content.toDoubleOrNull()

    /**
     * Returns content of current element as float
     * @throws NumberFormatException if current element is not a valid representation of number
     */
    val float: Float get() = content.toFloat()

    /**
     * Returns content of current element as float or `null` if current element is not a valid representation of number
     */
    val floatOrNull: Float? get() = content.toFloatOrNull()

    /**
     * Returns content of current element as boolean
     * @throws IllegalStateException if current element doesn't represent boolean
     */
    val boolean: Boolean get() = content.toBooleanStrict()

    /**
     * Returns content of current element as boolean or `null` if current element is not a valid representation of boolean
     */
    val booleanOrNull: Boolean? get() = content.toBooleanStrictOrNull()

    override fun toString() = content
}

/**
 * Class representing JSON literals: numbers, booleans and string.
 * Strings are always quoted.
 */
data class JsonLiteral internal constructor(
        private val body: Any,
        private val isString: Boolean
) : JsonPrimitive() {

    override val content = body.toString()
    override val contentOrNull: String = content

    /**
     * Creates number literal
     */
    constructor(number: Number) : this(number, false)

    /**
     * Creates boolean literal
     */
    constructor(boolean: Boolean) : this(boolean, false)

    /**
     * Creates quoted string literal
     */
    constructor(string: String) : this(string, true)

    override fun toString() =
            if (isString) buildString { printQuoted(content) }
            else content

    fun unquoted() = content
}

/**
 * Class representing JSON `null` value
 */
object JsonNull : JsonPrimitive() {
    override val jsonNull: JsonNull = this
    override val content: String = "null"
    override val contentOrNull: String? = null
}

/**
 * Class representing JSON object, consisting of name-value pairs, where value is arbitrary [JsonElement]
 */
data class JsonObject(val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {

    override val jsonObject: JsonObject = this

    /**
     * Returns [JsonElement] associated with given [key]
     * @throws NoSuchElementException if element is not present
     */
    override fun get(key: String): JsonElement = content[key] ?: throw NoSuchElementException("Element $key is missing")

    /**
     * Returns [JsonElement] associated with given [key] or `null` if element is not present
     */
    fun getOrNull(key: String): JsonElement? = content[key]

    /**
     * Returns [JsonPrimitive] associated with given [key]
     *
     * @throws NoSuchElementException if element is not present
     * @throws JsonElementTypeMismatchException if element is present, but has invalid type
     */
    fun getPrimitive(key: String): JsonPrimitive = get(key) as? JsonPrimitive
            ?: unexpectedJson(key, "JsonPrimitive")

    /**
     * Returns [JsonObject] associated with given [key]
     *
     * @throws NoSuchElementException if element is not present
     * @throws JsonElementTypeMismatchException if element is present, but has invalid type
     */
    fun getObject(key: String): JsonObject = get(key) as? JsonObject
            ?: unexpectedJson(key, "JsonObject")

    /**
     * Returns [JsonArray] associated with given [key]
     *
     * @throws NoSuchElementException if element is not present
     * @throws JsonElementTypeMismatchException if element is present, but has invalid type
     */
    fun getArray(key: String): JsonArray = get(key) as? JsonArray
            ?: unexpectedJson(key, "JsonArray")

    /**
     * Returns [JsonPrimitive] associated with given [key] or `null` if element
     * is not present or has different type
     */
    fun getPrimitiveOrNull(key: String): JsonPrimitive? = content[key] as? JsonPrimitive

    /**
     * Returns [JsonObject] associated with given [key] or `null` if element
     * is not present or has different type
     */
    fun getObjectOrNull(key: String): JsonObject? = content[key] as? JsonObject

    /**
     * Returns [JsonArray] associated with given [key] or `null` if element
     * is not present or has different type
     */
    fun getArrayOrNull(key: String): JsonArray? = content[key] as? JsonArray

    /**
     * Returns [J] associated with given [key]
     *
     * @throws NoSuchElementException if element is not present
     * @throws JsonElementTypeMismatchException if element is present, but has invalid type
     */
    inline fun <reified J : JsonElement> getAs(key: String): J = get(key) as? J
            ?: unexpectedJson(key, J::class.toString())

    /**
     * Returns [J] associated with given [key] or `null` if element
     * is not present or has different type
     */
    inline fun <reified J : JsonElement> lookup(key: String): J? = content[key] as? J

    override fun toString(): String {
        return content.entries.joinToString(
                prefix = "{",
                postfix = "}",
                transform = {(k, v) -> """"$k": $v"""}
        )
    }
}

data class JsonArray(val content: List<JsonElement>) : JsonElement(), List<JsonElement> by content {

    override val jsonArray: JsonArray = this

    /**
     * Returns [index]-th element of an array as [JsonPrimitive]
     * @throws JsonElementTypeMismatchException if element has invalid type
     */
    fun getPrimitive(index: Int) = content[index] as? JsonPrimitive
            ?: unexpectedJson("at $index", "JsonPrimitive")

    /**
     * Returns [index]-th element of an array as [JsonObject]
     * @throws JsonElementTypeMismatchException if element has invalid type
     */
    fun getObject(index: Int) = content[index] as? JsonObject
            ?: unexpectedJson("at $index", "JsonObject")

    /**
     * Returns [index]-th element of an array as [JsonArray]
     * @throws JsonElementTypeMismatchException if element has invalid type
     */
    fun getArray(index: Int) = content[index] as? JsonArray
            ?: unexpectedJson("at $index", "JsonArray")

    /**
     * Returns [index]-th element of an array as [JsonPrimitive] or `null` if element is missing or has different type
     */
    fun getPrimitiveOrNull(index: Int) = content.getOrNull(index) as? JsonPrimitive

    /**
     * Returns [index]-th element of an array as [JsonObject] or `null` if element is missing or has different type
     */
    fun getObjectOrNull(index: Int) = content.getOrNull(index) as? JsonObject

    /**
     * Returns [index]-th element of an array as [JsonArray] or `null` if element is missing or has different type
     */
    fun getArrayOrNull(index: Int) = content.getOrNull(index) as? JsonArray

    /**
     * Returns [index]-th element of an array as [J]
     * @throws JsonElementTypeMismatchException if element has invalid type
     */
    inline fun <reified J : JsonElement> getAs(index: Int): J = content[index] as? J
            ?: unexpectedJson("at $index", J::class.toString())

    /**
     * Returns [index]-th element of an array as [J] or `null` if element is missing or has different type
     */
    inline fun <reified J : JsonElement> getAsOrNull(index: Int): J? = content.getOrNull(index) as? J

    override fun toString() = content.joinToString(prefix = "[", postfix = "]")
}

fun unexpectedJson(key: String, expected: String): Nothing =
        throw JsonElementTypeMismatchException(key, expected)