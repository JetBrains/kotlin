/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import java.util.AbstractSet

/**
 * Provides a skeletal implementation of the [MutableSet] interface.
 *
 * @param E the type of elements contained in the set. The set is invariant in its element type.
 */
@SinceKotlin("1.1")
public actual abstract class AbstractMutableSet<E> protected actual constructor() : MutableSet<E>, AbstractSet<E>() {
    /**
     * Adds the specified element to the set.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    actual abstract override fun add(element: E): Boolean
}