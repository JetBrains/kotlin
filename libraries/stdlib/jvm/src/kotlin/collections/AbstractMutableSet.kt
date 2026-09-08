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
     * If the set doesn't contain [element], it is added to the set and the function returns `true`.
     * If the set already contains [element], the element instance stored in the set is kept, [element] is not
     * added, and the function returns `false`.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     */
    @IgnorableReturnValue
    actual abstract override fun add(element: E): Boolean
}
