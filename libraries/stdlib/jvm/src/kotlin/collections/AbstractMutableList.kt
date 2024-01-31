/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import java.util.AbstractList

/**
 * Provides a skeletal implementation of the [MutableList] interface.
 *
 * @param E the type of elements contained in the list. The list is invariant in its element type.
 */
@SinceKotlin("1.1")
// removeRange, modCount: Kotlin `protected` visibility is different from Java
@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")
public actual abstract class AbstractMutableList<E> protected actual constructor() : MutableList<E>, AbstractList<E>() {
    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.

     * @return the element previously at the specified position.
     */
    abstract override fun set(index: Int, element: E): E

    /**
     * Removes an element at the specified [index] from the list.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return the element that has been removed.
     */
    abstract override fun removeAt(index: Int): E

    /**
     * Inserts an element into the list at the specified [index].
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     */
    abstract override fun add(index: Int, element: E)
}