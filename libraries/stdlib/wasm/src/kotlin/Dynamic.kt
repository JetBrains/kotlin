/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER") // TODO: Remove after bootstrap update

package kotlin.js

@PublishedApi
internal fun dynamicSetBoolean(obj: Dynamic, index: String, value: Boolean): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetByte(obj: Dynamic, index: String, value: Byte): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetShort(obj: Dynamic, index: String, value: Short): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetChar(obj: Dynamic, index: String, value: Char): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetInt(obj: Dynamic, index: String, value: Int): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetLong(obj: Dynamic, index: String, value: Long): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetFloat(obj: Dynamic, index: String, value: Float): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetDouble(obj: Dynamic, index: String, value: Double): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetString(obj: Dynamic, index: String, value: String?): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicSetAny(obj: Dynamic, index: String, value: Any?): Unit =
    js("obj[index] = value")

@PublishedApi
internal fun dynamicGetBoolean(obj: Dynamic, index: String): Boolean =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetByte(obj: Dynamic, index: String): Byte =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetShort(obj: Dynamic, index: String): Short =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetChar(obj: Dynamic, index: String): Char =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetInt(obj: Dynamic, index: String): Int =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetLong(obj: Dynamic, index: String): Long =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetFloat(obj: Dynamic, index: String): Float =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetDouble(obj: Dynamic, index: String): Double =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetString(obj: Dynamic, index: String): String? =
    js("obj[index]")

@PublishedApi
internal fun dynamicGetAny(obj: Dynamic, index: String): Any? =
    js("obj[index]")

@PublishedApi
internal fun Dynamic.getBoolean(index: String): Boolean = dynamicGetBoolean(this, index)

@PublishedApi
internal fun Dynamic.getByte(index: String): Byte = dynamicGetByte(this, index)

@PublishedApi
internal fun Dynamic.getShort(index: String): Short = dynamicGetShort(this, index)

@PublishedApi
internal fun Dynamic.getChar(index: String): Char = dynamicGetChar(this, index)

@PublishedApi
internal fun Dynamic.getInt(index: String): Int = dynamicGetInt(this, index)

@PublishedApi
internal fun Dynamic.getLong(index: String): Long = dynamicGetLong(this, index)

@PublishedApi
internal fun Dynamic.getFloat(index: String): Float = dynamicGetFloat(this, index)

@PublishedApi
internal fun Dynamic.getDouble(index: String): Double = dynamicGetDouble(this, index)

@PublishedApi
internal fun Dynamic.getString(index: String): String? = dynamicGetString(this, index)

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> Dynamic.getAny(index: String): T? = dynamicGetAny(this, index) as T?

@PublishedApi
internal fun Dynamic.getBoolean(index: Int): Boolean = dynamicGetBoolean(this, index.toString())

@PublishedApi
internal fun Dynamic.getByte(index: Int): Byte = dynamicGetByte(this, index.toString())

@PublishedApi
internal fun Dynamic.getShort(index: Int): Short = dynamicGetShort(this, index.toString())

@PublishedApi
internal fun Dynamic.getChar(index: Int): Char = dynamicGetChar(this, index.toString())

@PublishedApi
internal fun Dynamic.getInt(index: Int): Int = dynamicGetInt(this, index.toString())

@PublishedApi
internal fun Dynamic.getLong(index: Int): Long = dynamicGetLong(this, index.toString())

@PublishedApi
internal fun Dynamic.getFloat(index: Int): Float = dynamicGetFloat(this, index.toString())

@PublishedApi
internal fun Dynamic.getDouble(index: Int): Double = dynamicGetDouble(this, index.toString())

@PublishedApi
internal fun Dynamic.getString(index: Int): String? = dynamicGetString(this, index.toString())

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> Dynamic.getAny(index: Int): T? = dynamicGetAny(this, index.toString()) as T?

/**
 * Represents unversal type for JS interoperability.
 */
external interface Dynamic

/**
 * Reinterprets this value as a value of the Dynamic type.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
@kotlin.internal.InlineOnly
fun Any.asDynamic(): Dynamic = this as Dynamic

/**
 * Reinterprets this value as a value of the Dynamic type.
 */
@kotlin.internal.InlineOnly
fun String.asDynamic(): Dynamic = this.unsafeCast<Dynamic>()

operator fun Dynamic.set(index: String, value: Any?) {
    when (value) {
        is Boolean -> dynamicSetBoolean(this, index, value)
        is Byte -> dynamicSetByte(this, index, value)
        is Short -> dynamicSetShort(this, index, value)
        is Char -> dynamicSetChar(this, index, value)
        is Int -> dynamicSetInt(this, index, value)
        is Long -> dynamicSetLong(this, index, value)
        is Float -> dynamicSetFloat(this, index, value)
        is Double -> dynamicSetDouble(this, index, value)
        is String -> dynamicSetString(this, index, value)
        else -> dynamicSetAny(this, index, value)
    }
}

operator fun Dynamic.set(index: Int, value: Any?) {
    this[index.toString()] = value
}

private fun <T> unsafeCastJs(x: String): Dynamic = js("x")

@Suppress("UNCHECKED_CAST")
fun <T> String.unsafeCast(): T = unsafeCastJs<T>(this) as T

private fun <T> unsafeCastJs(x: Boolean): Dynamic = js("x")

/**
 * Reinterprets boolean value as a value of the specified type [T] without any actual type checking.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Boolean.unsafeCast(): T = unsafeCastJs<T>(this) as T

/**
 * Reinterprets Nothing? type value as Dynamic? null.
 */
fun <T> Nothing?.unsafeCast(): Dynamic? = null

/**
 * Reinterprets Dynamic type value as a value of the specified type [T] without any actual type checking.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Dynamic.unsafeCast(): T = this as T

private fun jsThrow(e: Dynamic) {
    js("throw e;")
}

private fun jsCatch(f: () -> Unit): Dynamic? {
    js("""
    let result = null;
    try { 
        f();
    } catch (e) {
       result = e;
    }
    return result;
    """)
}

/**
 * For a Dynamic value caught in JS, returns the corresponding [Throwable]
 * if it was thrown from Kotlin, or null otherwise.
 */
public fun Dynamic.toThrowableOrNull(): Throwable? {
    val thisAny: Any = this
    if (thisAny is Throwable) return thisAny
    var result: Throwable? = null
    jsCatch {
        try {
            jsThrow(this)
        } catch (e: Throwable) {
            result = e
        }
    }
    return result
}
