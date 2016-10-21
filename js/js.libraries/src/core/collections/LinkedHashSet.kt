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
/*
 * Based on GWT LinkedHashSet
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

/**
 * The implementation of the [MutableSet] interface, backed by a [LinkedHashMap] instance.
 *
 * This implementation preserves the insertion order of elements during the iteration.
 */
public open class LinkedHashSet<E> : HashSet<E> {

    internal constructor(map: LinkedHashMap<E, Any>) : super(map)

    /**
     * Constructs a new empty [LinkedHashSet].
     */
    constructor() : super(LinkedHashMap<E, Any>())

    /**
     * Constructs a new [LinkedHashSet] filled with the elements of the specified collection.
     */
    constructor(elements: Collection<E>) : super(LinkedHashMap<E, Any>()) {
        addAll(elements)
    }
    /**
     * Constructs a new empty [LinkedHashSet].
     *
     * @param  initialCapacity the initial capacity (ignored)
     * @param  loadFactor      the load factor (ignored)
     *
     * @throws IllegalArgumentException if the initial capacity or load factor are negative
     */
    constructor(initialCapacity: Int, loadFactor: Float = 0.0f) : super(LinkedHashMap<E, Any>(initialCapacity, loadFactor))

//    public override fun clone(): Any {
//        return LinkedHashSet(this)
//    }

}

/**
 * Creates a new instance of the specialized implementation of [LinkedHashSet] with the specified [String] elements,
 * which elements the keys as properties of JS object without hashing them.
 */
public fun linkedStringSetOf(vararg elements: String): LinkedHashSet<String> {
    return LinkedHashSet(linkedStringMapOf<Any>()).apply { addAll(elements) }
}
