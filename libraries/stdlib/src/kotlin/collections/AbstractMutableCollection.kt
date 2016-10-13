@file:JvmVersion
package kotlin.collections

import java.util.AbstractCollection

@SinceKotlin("1.1")
public abstract class AbstractMutableCollection<E> protected constructor() : MutableCollection<E>, AbstractCollection<E>() {
    abstract override fun add(element: E): Boolean
}