/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")

package kotlin.collections

import java.nio.charset.Charset


/**
 * Returns the array if it's not `null`, or an empty array otherwise.
 * @sample samples.collections.Arrays.Usage.arrayOrEmpty
 */
public actual inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()

/**
 * Converts the contents of this byte array to a string using the specified [charset].
 * @sample samples.text.Strings.stringToByteArray
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.toString(charset: Charset): String = String(this, charset)

/**
 * Returns a *typed* array containing all of the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 * @sample samples.collections.Collections.Collections.collectionToTypedArray
 */
@Suppress("UNCHECKED_CAST")
public actual inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val thisCollection = this as java.util.Collection<T>
    return thisCollection.toArray(arrayOfNulls<T>(0)) as Array<T>
}

/** Internal unsafe construction of array based on reference array type */
internal actual fun <T> arrayOfNulls(reference: Array<T>, size: Int): Array<T> {
    @Suppress("UNCHECKED_CAST")
    return java.lang.reflect.Array.newInstance(reference.javaClass.componentType, size) as Array<T>
}

@SinceKotlin("1.3")
internal fun copyOfRangeToIndexCheck(toIndex: Int, size: Int) {
    if (toIndex > size) throw IndexOutOfBoundsException("toIndex ($toIndex) is greater than size ($size).")
}


@SinceKotlin("1.3")
@PublishedApi
@kotlin.jvm.JvmName("contentDeepHashCode")
internal fun <T> Array<out T>.contentDeepHashCodeImpl(): Int =
// returns valid result for unsigned arrays by accident:
// hash code of an inline class, which an unsigned array is,
// is calculated structurally as in a data class
    java.util.Arrays.deepHashCode(this)
