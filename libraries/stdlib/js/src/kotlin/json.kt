/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * An interface for indexing access to a collection of key-value pairs, where type of key is [String] and type of value is [Any?][Any].
 */
public external interface Json {
    /**
     * Calls to the function will be translated to indexing operation (square brackets) on the receiver with [propertyName] as the argument.
     *
     * E.g. for next code:
     * ```kotlin
     * fun test(j: Json, p: String) = j["prop"] + j.get(p)
     * ```
     *
     * will be generated:
     * ```js
     * function test(j, p) {
     *     return j["prop"] + j[p];
     * }
     * ```
     */
    public operator fun get(propertyName: String): Any?

    /**
     * Calls of the function will be translated to an assignment of [value] to the receiver indexed (with square brackets/index operation) with [propertyName].
     *
     * E.g. for the following code:
     * ```kotlin
     * fun test(j: Json, p: String, newValue: Any) {
     *     j["prop"] = 1
     *     j.set(p, newValue)
     * }
     * ```
     *
     * will be generated:
     * ```js
     * function test(j, p, newValue) {
     *     j["prop"] = 1;
     *     j[p] = newValue;
     * }
     * }
     * ```
     */
    public operator fun set(propertyName: String, value: Any?): Unit
}

/**
 * Returns a simple JavaScript object (as [Json]) using provided key-value pairs as names and values of its properties.
 */
public fun json(vararg pairs: Pair<String, Any?>): Json {
    val res: dynamic = js("({})")
    for ((name, value) in pairs) {
        res[name] = value
    }
    return res
}

/**
 * Adds key-value pairs from [other] to [this].
 * Returns the original receiver.
 */
public fun Json.add(other: Json): Json {
    val keys: Array<String> = js("Object").keys(other)
    for (key in keys) {
        if (other.asDynamic().hasOwnProperty(key)) {
            this[key] = other[key];
        }
    }
    return this
}

/**
 * Exposes the JavaScript [JSON object](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON) to Kotlin.
 */
@Suppress("NOT_DOCUMENTED")
public external object JSON {
    public fun stringify(o: Any?): String
    public fun stringify(o: Any?, replacer: ((key: String, value: Any?) -> Any?)): String
    public fun stringify(o: Any?, replacer: ((key: String, value: Any?) -> Any?)? = definedExternally, space: Int): String
    public fun stringify(o: Any?, replacer: ((key: String, value: Any?) -> Any?)? = definedExternally, space: String): String
    public fun stringify(o: Any?, replacer: Array<String>): String
    public fun stringify(o: Any?, replacer: Array<String>, space: Int): String
    public fun stringify(o: Any?, replacer: Array<String>, space: String): String

    public fun <T> parse(text: String): T
    public fun <T> parse(text: String, reviver: ((key: String, value: Any?) -> Any?)): T
}
