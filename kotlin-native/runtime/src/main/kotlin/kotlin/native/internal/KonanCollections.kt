/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

internal interface KonanSet<out E> : Set<E> {
    /**
     * Searches for the specified element in this set.
     *
     * @return the element from the set equal to [element], or `null` if no such element found.
     */
    fun getElement(element: @UnsafeVariance E): E?
}
