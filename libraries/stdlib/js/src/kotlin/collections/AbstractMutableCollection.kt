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
 * Provides a skeletal implementation of the [MutableCollection] interface.
 *
 * @param E the type of elements contained in the collection. The collection is invariant on its element type.
 */
public abstract class AbstractMutableCollection<E> protected constructor() : AbstractCollection<E>(), MutableCollection<E> {

    abstract override fun add(element: E): Boolean

    override fun remove(element: E): Boolean {
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == element) {
                iterator.remove()
                return true
            }
        }
        return false
    }

    override fun addAll(elements: Collection<E>): Boolean {
        var modified = false
        for (element in elements) {
            if (add(element)) modified = true
        }
        return modified
    }

    override fun removeAll(elements: Collection<E>): Boolean = (this as MutableIterable<E>).removeAll { it in elements }
    override fun retainAll(elements: Collection<E>): Boolean = (this as MutableIterable<E>).removeAll { it !in elements }

    override fun clear(): Unit {
        val iterator = this.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    open fun toJSON(): Any = this.toArray()
}

