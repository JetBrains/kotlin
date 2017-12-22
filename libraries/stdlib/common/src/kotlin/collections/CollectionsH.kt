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

package kotlin.collections

expect interface RandomAccess

/** Returns the array if it's not `null`, or an empty array otherwise. */
expect inline fun <reified T> Array<out T>?.orEmpty(): Array<out T>


expect inline fun <reified T> Collection<T>.toTypedArray(): Array<T>

@SinceKotlin("1.2")
expect fun <T> MutableList<T>.fill(value: T): Unit
@SinceKotlin("1.2")
expect fun <T> MutableList<T>.shuffle(): Unit
@SinceKotlin("1.2")
expect fun <T> Iterable<T>.shuffled(): List<T>

expect fun <T : Comparable<T>> MutableList<T>.sort(): Unit
expect fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit


// from Grouping.kt
public expect fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int>
// public expect inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int>

internal expect fun copyToArrayImpl(collection: Collection<*>): Array<Any?>

internal expect fun <T> copyToArrayImpl(collection: Collection<*>, array: Array<T>): Array<T>

internal expect fun <T> arrayOfNulls(reference: Array<T>, size: Int): Array<T>
internal expect fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V>
internal expect fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V>
internal expect fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?>
