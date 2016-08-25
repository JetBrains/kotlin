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
 * Based on GWT HashSet
 * Copyright 2008 Google Inc.
 */

package kotlin.collections


/**
 * Implements a set in terms of a hash table. [[Sun
   * docs]](http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashSet.html)

 * @param  element type.
 */
open class HashSet<E> : AbstractSet<E>, Set<E>, Cloneable, Serializable {

    @Transient private var map: HashMap<E, Any>? = null

    /**
     * Ensures that RPC will consider type parameter E to be exposed. It will be
     * pruned by dead code elimination.
     */
    @SuppressWarnings("unused")
    private val exposeElement: E? = null

    constructor() {
        map = HashMap<E, Any>()
    }

    constructor(c: Collection<E>) {
        map = HashMap<E, Any>(c.size)
        addAll(c)
    }

    constructor(initialCapacity: Int) {
        map = HashMap<E, Any>(initialCapacity)
    }

    constructor(initialCapacity: Int, loadFactor: Float) {
        map = HashMap<E, Any>(initialCapacity, loadFactor)
    }

    /**
     * Protected constructor to specify the underlying map. This is used by
     * LinkedHashSet.

     * @param map underlying map to use.
     */
    protected constructor(map: HashMap<E, Any>) {
        this.map = map
    }

    override fun add(o: E?): Boolean {
        val old = map!!.put(o, this)
        return old == null
    }

    override fun clear() {
        map!!.clear()
    }

    public override fun clone(): Any {
        return HashSet<E>(this)
    }

    override operator fun contains(o: Any?): Boolean {
        return map!!.containsKey(o)
    }

    override fun isEmpty(): Boolean {
        return map!!.isEmpty()
    }

    override fun iterator(): Iterator<E> {
        return map!!.keys.iterator()
    }

    override fun remove(o: Any?): Boolean {
        return map!!.remove(o) != null
    }

    override fun size(): Int {
        return map!!.size
    }

}
