// DUMP_KT_IR
// FILE: test.kt

import lombok.EqualsAndHashCode
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

open class Base(val baseProp: Int) {
    // An unrelated generic overload declared before the real `equals`: `super.equals(other)` must not resolve to it.
    fun <T : CharSequence> equals(other: T): Boolean = false

    override fun equals(other: Any?): Boolean = other is Base && other.baseProp == baseProp
    override fun hashCode(): Int = baseProp
}

@EqualsAndHashCode(callSuper = true)
class Derived(val ownProp: String, baseProp: Int) : Base(baseProp)

fun box(): String {
    val derived = Derived("same", 1)
    assertEquals(Derived("same", 1), derived, "chained equals rejects an equal instance")
    assertNotEquals(Derived("same", 2), derived, "equals did not chain to Base")
    return "OK"
}
