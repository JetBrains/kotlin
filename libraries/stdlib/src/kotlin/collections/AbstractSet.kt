/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.collections

/**
 * Provides a skeletal implementation of the read-only [Set] interface.
 *
 * This class is intended to help implementing read-only sets so it doesn't support concurrent modification tracking.
 *
 * @param E the type of elements contained in the set. The set is covariant in its element type.
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