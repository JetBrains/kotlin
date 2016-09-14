@file:JvmVersion
package kotlin.collections

import java.util.AbstractCollection

public abstract class AbstractMutableCollection<E> protected constructor() : AbstractCollection<E>() {
    abstract override fun add(element: E): Boolean
}