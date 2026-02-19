// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// example from https://github.com/Kotlin/kotlinx.serialization/issues/1736
@Serializable
sealed class ValueParent {
    @Serializable
    class ValueChild(val i: Int) : ValueParent()
}

@Serializable
sealed class HolderParent<T : ValueParent> {
    abstract val a: ValueParent

    @Serializable
    class Holder<T : ValueParent>(
        override val a: T,
    ) : HolderParent<T>()
}

// The hierarchy depth is greater than 1 and type parameters indexes are changing
@Serializable
class C<X>(val cX: X) : B<Long, X>(4242L, cX)

@Serializable
sealed class B<X, Y>(val bX: X, val bY: Y) : A<Int, String, Y>(42, "43", bY)

@Serializable
sealed class A<X, Y, Z>(val aX: X, val aY: Y, val aZ: Z)


fun box(): String {
    val holder = HolderParent.Holder<ValueParent>(ValueParent.ValueChild(42))
    val encoded = Json.encodeToString<HolderParent<ValueParent>>(holder)
    if (encoded != """{"type":"HolderParent.Holder","a":{"type":"ValueParent.ValueChild","i":42}}""") return encoded

    val deepEncoded = Json.encodeToString<A<Int, String, ValueParent>>(C(ValueParent.ValueChild(0)))
    if (deepEncoded != """{"type":"C","aX":42,"aY":"43","aZ":{"type":"ValueParent.ValueChild","i":0},"bX":4242,"bY":{"type":"ValueParent.ValueChild","i":0},"cX":{"type":"ValueParent.ValueChild","i":0}}""") return deepEncoded

    return "OK"
}
