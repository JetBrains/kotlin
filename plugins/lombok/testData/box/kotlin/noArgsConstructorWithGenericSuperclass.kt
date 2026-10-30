// DUMP_KT_IR
// FILE: test.kt

import lombok.NoArgsConstructor
import kotlin.test.assertEquals
import kotlin.test.assertNull

open class Base<T> {
    var t: T? = null
}

open class Base2<K, V> {
    var k: K? = null
    var v: V? = null
}

// The generated no-args constructor delegates to `Base<String>()`: its super call must carry `<String>`.
// Currently the type arguments of all generated super calls below are missing, see the `.kt.txt` dump.
@NoArgsConstructor
class Derived(var x: Int) : Base<String>()

// The same for two type arguments, `<Int, String>`.
@NoArgsConstructor
class Derived2(var x: Int) : Base2<Int, String>()

// A type argument referring to the class's own type parameter.
@NoArgsConstructor
class DerivedGeneric<E>(var e: E) : Base<List<E>>()

fun box(): String {
    val derived = Derived()
    assertEquals(0, derived.x)
    assertNull(derived.t)
    derived.t = "O"

    val derived2 = Derived2()
    assertNull(derived2.k)
    derived2.v = "K"

    val derivedGeneric = DerivedGeneric<Int>()
    assertNull(derivedGeneric.e)
    derivedGeneric.t = listOf(1)

    return derived.t + derived2.v
}
