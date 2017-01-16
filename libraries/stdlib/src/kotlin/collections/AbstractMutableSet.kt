@file:JvmVersion
package kotlin.collections

import java.util.AbstractSet

@SinceKotlin("1.1")
public abstract class AbstractMutableSet<E> protected constructor() : MutableSet<E>, AbstractSet<E>() {
    abstract override fun add(element: E): Boolean
}