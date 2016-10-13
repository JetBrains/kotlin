@file:JvmVersion
package kotlin.collections

import java.util.AbstractSet

@SinceKotlin("1.1")
public abstract class AbstractMutableSet<E> protected constructor() : MutableSet<E>, AbstractSet<E>() {
    // nothing to make abstract
    // it's a class rather than typealias in order to have bridge for `size` generated and nice non-platform types in methods
}