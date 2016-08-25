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
 * Based on GWT HashMap
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

open class HashMap<K, V> : AbstractHashMap<K, V>, Cloneable, Serializable {

    /**
     * Ensures that RPC will consider type parameter K to be exposed. It will be
     * pruned by dead code elimination.
     */
    @SuppressWarnings("unused")
    private val exposeKey: K? = null

    /**
     * Ensures that RPC will consider type parameter V to be exposed. It will be
     * pruned by dead code elimination.
     */
    @SuppressWarnings("unused")
    private val exposeValue: V? = null

    constructor() {
    }

    constructor(ignored: Int) : super(ignored) {
    }

    constructor(ignored: Int, alsoIgnored: Float) : super(ignored, alsoIgnored) {
    }

    constructor(toBeCopied: Map<out K, V>) : super(toBeCopied) {
    }

    public override fun clone(): Any {
        return HashMap<K, V>(this)
    }

    internal fun equals(value1: Any, value2: Any): Boolean {
        return value1 == value2
    }

    internal fun getHashCode(key: Any): Int {
        val hashCode = key.hashCode()
        // Coerce to int -- our classes all do this, but a user-written class might not.
        return ensureInt(hashCode)
    }
}
