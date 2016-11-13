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

package java.util


// TODO: Not supported
// typealias Date = kotlin.js.Date

@native
public class Date() {
    public fun getTime(): Int = noImpl
}

// TODO: Deprecate with replacement
public typealias RandomAccess = kotlin.collections.RandomAccess
public typealias ArrayList<E> = kotlin.collections.ArrayList<E>
public typealias HashSet<E> = kotlin.collections.HashSet<E>
public typealias LinkedHashSet<E> = kotlin.collections.LinkedHashSet<E>
public typealias HashMap<K, V> = kotlin.collections.HashMap<K, V>
public typealias LinkedHashMap<K, V> = kotlin.collections.LinkedHashMap<K, V>

@Deprecated("Use AbstractCollection or AbstractMutableCollection from kotlin.collections", ReplaceWith("kotlin.collections.AbstractMutableCollection<E>"))
public abstract class AbstractCollection<E> : kotlin.collections.AbstractMutableCollection<E>() {
    override fun add(element: E): Boolean = throw UnsupportedOperationException()
}

@Deprecated("Use AbstractList or AbstractMutableList from kotlin.collections", ReplaceWith("kotlin.collections.AbstractMutableList<E>"))
public abstract class AbstractList<E> : kotlin.collections.AbstractMutableList<E>() {
    override fun add(index: Int, element: E): Unit = throw UnsupportedOperationException()
    override fun removeAt(index: Int): E = throw UnsupportedOperationException()
    override fun set(index: Int, element: E): E = throw UnsupportedOperationException()
}
