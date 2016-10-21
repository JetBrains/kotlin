@file:JvmVersion
package kotlin.collections

import java.util.AbstractCollection

/**
 * Provides a skeletal implementation of the [MutableCollection] interface.
 *
 * @param E the type of elements contained in the collection. The collection is invariant on its element type.
 */
@SinceKotlin("1.1")
public abstract class AbstractMutableCollection<E> protected constructor() : MutableCollection<E>, AbstractCollection<E>() {
    /**
     * Adds the specified element to the collection.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return `true` if the element has been added, `false` if the collection does not support duplicates
     * and the element is already contained in the collection.
     */
    abstract override fun add(element: E): Boolean
}