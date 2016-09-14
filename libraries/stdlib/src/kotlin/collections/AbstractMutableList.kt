@file:JvmVersion
package kotlin.collections

import java.util.AbstractList

public abstract class AbstractMutableList<E> protected constructor() : AbstractList<E>() {
    abstract override fun set(index: Int, element: E): E
    abstract override fun removeAt(index: Int): E
    abstract override fun add(index: Int, element: E)
}