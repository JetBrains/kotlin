/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package java.util

public fun HashSet<E>(c: Collection<E>): HashSet<E>
        = HashSet<E>(c.size()).apply { addAll(c) }

public fun LinkedHashSet<E>(c: Collection<E>): HashSet<E>
        = LinkedHashSet<E>(c.size()).apply { addAll(c) }

public fun HashMap<K, V>(m: Map<K, V>): HashMap<K, V>
        = HashMap<K, V>(m.size()).apply { putAll(m) }

public fun LinkedHashMap<K, V>(m: Map<K, V>): LinkedHashMap<K, V>
        = LinkedHashMap<K, V>(m.size()).apply { putAll(m) }

public fun ArrayList<E>(c: Collection<E>): ArrayList<E>
        = ArrayList<E>().apply { asDynamic().array = c.toTypedArray<Any?>() } // black dynamic magic

