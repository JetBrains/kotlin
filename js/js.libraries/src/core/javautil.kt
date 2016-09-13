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

@library
public interface Comparator<T> {
    public fun compare(obj1: T, obj2: T): Int
}

public inline fun <T> Comparator(crossinline comparison: (T, T) -> Int): Comparator<T> = object : Comparator<T> {
    override fun compare(obj1: T, obj2: T): Int = comparison(obj1, obj2)
}



@native
public class Date() {
    public fun getTime(): Int = noImpl
}

// TODO: Deprecate with replacement
public typealias RandomAccess = kotlin.collections.RandomAccess
public typealias AbstractCollection<E> = kotlin.collections.AbstractCollection<E>
public typealias AbstractList<E> = kotlin.collections.AbstractList<E>
public typealias ArrayList<E> = kotlin.collections.ArrayList<E>
public typealias HashSet<E> = kotlin.collections.HashSet<E>
public typealias LinkedHashSet<E> = kotlin.collections.LinkedHashSet<E>
public typealias HashMap<K, V> = kotlin.collections.HashMap<K, V>
public typealias LinkedHashMap<K, V> = kotlin.collections.LinkedHashMap<K, V>