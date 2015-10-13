/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

@file:kotlin.jvm.JvmName("DeprecatedBuiltinsKt")

package kotlin

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun Collection<*>.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun Map<*, *>.size() = size

@Deprecated("Use property 'isEmpty' instead", ReplaceWith("this.isEmpty"))
public inline fun Collection<*>.isEmpty() = isEmpty

@Deprecated("Use property 'isEmpty' instead", ReplaceWith("this.isEmpty"))
public inline fun Map<*, *>.isEmpty() = isEmpty


@Deprecated("Use property 'key' instead", ReplaceWith("this.key"))
public fun <K, V> Map.Entry<K, V>.getKey(): K = key

@Deprecated("Use property 'value' instead", ReplaceWith("this.value"))
public fun <K, V> Map.Entry<K, V>.getValue(): V = value

@Deprecated("Use operator 'get' instead", ReplaceWith("this[index]"))
public fun CharSequence.charAt(index: Int): Char = this[index]

@Deprecated("Use 'removeAt' instead", ReplaceWith("this.removeAt(index)"))
public fun <E> MutableList<E>.remove(index: Int): E = removeAt(index)

@Deprecated("Use explicit cast to MutableCollection<Any?> instead", ReplaceWith("(this as MutableCollection<Any?>).remove(o)"))
public fun <E> MutableCollection<E>.remove(o: Any?): Boolean = remove(o as E)

@Deprecated("Use property 'length' instead", ReplaceWith("this.length"))
public fun CharSequence.length(): Int = length

@Deprecated("Use explicit cast to Map<Any?, V> instead", ReplaceWith("(this as Map<Any?, V>).get(o)"))
public inline operator fun <K, V> Map<K, V>.get(o: Any?): V? = get(o as K)

@Deprecated("Use explicit cast to Map<Any?, V> instead", ReplaceWith("(this as Map<Any?, V>).containsKey(o)"))
public inline fun <K, V> Map<K, V>.containsKey(o: Any?): Boolean = containsKey(o as K)

@Deprecated("Use explicit cast to Map<K, Any?> instead", ReplaceWith("(this as Map<K, Any?>).containsValue(o)"))
public inline fun <K, V> Map<K, V>.containsValue(o: Any?): Boolean = containsValue(o as V)

@Deprecated("Use property 'keys' instead", ReplaceWith("this.keys"))
public inline fun <K, V> Map<K, V>.keySet(): Set<K> = keys

@kotlin.jvm.JvmName("mutableKeys")
@Deprecated("Use property 'keys' instead", ReplaceWith("this.keys"))
public inline fun <K, V> MutableMap<K, V>.keySet(): MutableSet<K> = keys

@Deprecated("Use property 'entries' instead", ReplaceWith("this.entries"))
public inline fun <K, V> Map<K, V>.entrySet(): Set<Map.Entry<K, V>> = entries

@kotlin.jvm.JvmName("mutableEntrySet")
@Deprecated("Use property 'entries' instead", ReplaceWith("this.entries"))
public inline fun <K, V> MutableMap<K, V>.entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = entries

@Deprecated("Use property 'values' instead", ReplaceWith("this.values"))
public inline fun <K, V> Map<K, V>.values(): Collection<V> = values

@kotlin.jvm.JvmName("mutableValues")
@Deprecated("Use property 'values' instead", ReplaceWith("this.values"))
public inline fun <K, V> MutableMap<K, V>.values(): MutableCollection<V> = values
