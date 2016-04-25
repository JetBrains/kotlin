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

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")

package kotlin.collections

import java.nio.charset.Charset


/**
 * Converts the contents of this byte array to a string using the specified [charset].
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.toString(charset: Charset): String = String(this, charset)

/**
 * Returns a *typed* array containing all of the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val thisCollection = this as java.util.Collection<T>
    return thisCollection.toArray(arrayOfNulls<T>(thisCollection.size())) as Array<T>
}

/** Internal unsafe construction of array based on reference array type */
internal fun <T> arrayOfNulls(reference: Array<T>, size: Int): Array<T> {
    @Suppress("UNCHECKED_CAST")
    return java.lang.reflect.Array.newInstance(reference.javaClass.componentType, size) as Array<T>
}
