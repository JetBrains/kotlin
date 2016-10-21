@file:JvmVersion
package kotlin.collections

import java.util.AbstractSet

/**
 * Provides a skeletal implementation of the [MutableSet] interface.
 *
 * @param E the type of elements contained in the set. The set is invariant on its element type.
 */
@SinceKotlin("1.1")
public abstract class AbstractMutableSet<E> protected constructor() : MutableSet<E>, AbstractSet<E>() {
    /**
     * Adds the specified element to the set.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    abstract override fun add(element: E): Boolean
}