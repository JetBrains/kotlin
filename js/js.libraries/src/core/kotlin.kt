/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package kotlin

import java.util.*

/**
 * Returns an empty array of the specified type [T].
 */
public inline fun <reified T> emptyArray(): Array<T> = arrayOfNulls<T>(0) as Array<T>

@library
public fun <T> arrayOf(vararg elements: T): Array<T> = noImpl

@library
public fun doubleArrayOf(vararg elements: Double): DoubleArray = noImpl

@library
public fun floatArrayOf(vararg elements: Float): FloatArray = noImpl

@library
public fun longArrayOf(vararg elements: Long): LongArray = noImpl

@library
public fun intArrayOf(vararg elements: Int): IntArray = noImpl

@library
public fun charArrayOf(vararg elements: Char): CharArray = noImpl

@library
public fun shortArrayOf(vararg elements: Short): ShortArray = noImpl

@library
public fun byteArrayOf(vararg elements: Byte): ByteArray = noImpl

@library
public fun booleanArrayOf(vararg elements: Boolean): BooleanArray = noImpl

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 */
public fun <T> lazy(initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
public fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)


internal fun <T> arrayOfNulls(reference: Array<out T>, size: Int): Array<T> {
    return arrayOfNulls<Any>(size) as Array<T>
}

internal fun arrayCopyResize(source: dynamic, newSize: Int, defaultValue: Any?): dynamic {
    val result = source.slice(0, newSize)
    var index: Int = source.length
    if (newSize > index) {
        result.length = newSize
        while (index < newSize) result[index++] = defaultValue
    }
    return result
}

internal fun <T> arrayPlusCollection(array: dynamic, collection: Collection<T>): dynamic {
    val result = array.slice()
    result.length += collection.size
    var index: Int = array.length
    for (element in collection) result[index++] = element
    return result
}

// no singleton map implementation in js, return map as is
internal inline fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V> = this

internal inline fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V> = this.toMutableMap()

internal inline fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?> =
        if (isVarargs)
            // no need to copy vararg array in JS
            this
        else
            this.copyOf()

// temporary for shared code, until we have an annotation like JvmSerializable
internal interface Serializable