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

package kotlin.collections

public abstract class AbstractMutableCollection<E> protected constructor(): MutableCollection<E>, AbstractCollection<E>() {

    // Bulk Modification Operations
    /**
     * Adds all of the elements in the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    override public fun addAll(elements: Collection<E>): Boolean {
        var changed = false
        for (v in elements) {
            if (add(v)) changed = true
        }
        return changed
    }

    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
     */
    override fun remove(element: E): Boolean {
        val it = iterator()
        while (it.hasNext()) {
            if (it.next() == element) {
                it.remove()
                return true
            }
        }
        return false
    }

    /**
     * Removes all of this collection's elements that are also contained in the specified collection.
     *
     * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
     */
    override public fun removeAll(elements: Collection<E>): Boolean = (this as MutableIterable<E>).removeAll { it in elements }

    /**
     * Retains only the elements in this collection that are contained in the specified collection.
     *
     * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
     */
    override public fun retainAll(elements: Collection<E>): Boolean = (this as MutableIterable<E>).retainAll { it in elements }

    /**
     * Removes all elements from this collection.
     */
    override fun clear(): Unit {
        val it = iterator()
        while (it.hasNext()) {
            it.next()
            it.remove()
        }
    }
}
