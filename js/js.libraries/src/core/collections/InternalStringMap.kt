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
 * Based on GWT InternalStringMap
 * Copyright 2008 Google Inc.
 */
package kotlin.collections

/**
 * A simple wrapper around JavaScript Map for key type is string.
 */
internal class InternalStringMap<K, V>(private val host: AbstractHashMap<K, V>) : Iterable<Entry<K, V>> {

    private val backingMap = InternalJsMapFactory.newJsMap()
    private var size: Int = 0

    /**
     * A mod count to track 'value' replacements in map to ensure that the 'value' that we have in the
     * iterator entry is guaranteed to be still correct.
     * This is to optimize for the common scenario where the values are not modified during
     * iterations where the entries are never stale.
     */
    private var valueMod: Int = 0

    operator fun contains(key: String): Boolean {
        return !JsUtils.isUndefined(backingMap.get(key))
    }

    operator fun get(key: String): V {
        return backingMap.get(key)
    }

    fun put(key: String, value: V): V {
        val oldValue = backingMap.get(key)
        backingMap.set(key, toNullIfUndefined(value))

        if (JsUtils.isUndefined(oldValue)) {
            size++
            structureChanged(host)
        }
        else {
            valueMod++
        }
        return oldValue
    }

    fun remove(key: String): V {
        val value = backingMap.get(key)
        if (!JsUtils.isUndefined(value)) {
            backingMap.delete(key)
            size--
            structureChanged(host)
        }
        else {
            valueMod++
        }

        return value
    }

    fun size(): Int {
        return size
    }

    override fun iterator(): Iterator<Entry<K, V>> {
        return object : Iterator<Entry<K, V>> {
            internal var entries = backingMap.entries()
            internal var current = entries.next()
            internal var last: InternalJsMap.IteratorEntry<V>

            override fun hasNext(): Boolean {
                return !current.done
            }

            override fun next(): Entry<K, V> {
                last = current
                current = entries.next()
                return newMapEntry(last, valueMod)
            }

            override fun remove() {
                this@InternalStringMap.remove(last.getKey())
            }
        }
    }

    private fun newMapEntry(entry: InternalJsMap.IteratorEntry<V>,
                            lastValueMod: Int): Entry<K, V> {
        return object : AbstractMapEntry<K, V>() {
            val key: K
                @SuppressWarnings("unchecked")
                get() = entry.getKey()
            // Let's get a fresh copy as the value may have changed.
            val value: V
                get() {
                    if (valueMod != lastValueMod) {
                        return get(entry.getKey())
                    }
                    return entry.getValue()
                }

            fun setValue(`object`: V): V {
                return put(entry.getKey(), `object`)
            }
        }
    }

    private fun <T> toNullIfUndefined(value: T): T? {
        return if (JsUtils.isUndefined(value)) null else value
    }
}
