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

/**
 * The common interface of [InternalStringMap] and [InternalHashCodeMap].
 */
internal interface InternalMap<K, V> : MutableIterable<MutableMap.MutableEntry<K, V>> {
    val equality: EqualityComparator
    val size: Int
    operator fun contains(key: K): Boolean
    operator fun get(key: K): V?

    fun put(key: K, value: V): V?
    fun remove(key: K): V?
    fun clear(): Unit
}