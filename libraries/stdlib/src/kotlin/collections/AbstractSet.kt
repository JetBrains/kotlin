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
 * Provides a skeletal implementation of the read-only [Set] interface.
 *
 * This class is intended to help implementing read-only sets so it doesn't support concurrent modification tracking.
 *
 * @param E the type of elements contained in the set. The set is covariant on its element type.
 */
@SinceKotlin("1.1")
public abstract class AbstractSet<out E> protected constructor() : AbstractCollection<E>(), Set<E> {

    /**
     * Compares this set with other set instance with the unordered structural equality.
     *
     * @return true, if [other] instance is a [Set] of the same size, all elements of which are contained in this set.
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        return setEquals(this, other)
    }

    /**
     * Returns the hash code value for this set.
     */
    override fun hashCode(): Int = unorderedHashCode(this)

    internal companion object {
        internal fun unorderedHashCode(c: Collection<*>): Int {
            var hashCode = 0
            for (element in c) {
                hashCode += (element?.hashCode() ?: 0)
            }
            return hashCode
        }

        internal fun setEquals(c: Set<*>, other: Set<*>): Boolean {
            if (c.size != other.size) return false
            return c.containsAll(other)
        }
    }

}