@file:JvmVersion
package kotlin.collections

import java.util.AbstractSet

@SinceKotlin("1.1")
public abstract class AbstractMutableSet<E> protected constructor() : AbstractSet<E>() {
    // nothing to make abstract, maybe leave it typealias then?
}