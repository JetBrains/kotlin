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

@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmName("CollectionsJDK8Kt")
@file:kotlin.jvm.JvmPackageName("kotlin.collections.jdk8")
package kotlin.collections

/**
 * Returns the value to which the specified key is mapped, or
 * [defaultValue] if this map contains no mapping for the key.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes K, V> Map<out K, V>.getOrDefault(key: K, defaultValue: V): V
        = (this as Map<K, V>).getOrDefault(key, defaultValue)


/**
 * Removes the entry for the specified key only if it is currently
 * mapped to the specified value.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes K, @kotlin.internal.OnlyInputTypes V> MutableMap<out K, out V>.remove(key: K, value: V): Boolean
        = (this as MutableMap<K, V>).remove(key, value)
