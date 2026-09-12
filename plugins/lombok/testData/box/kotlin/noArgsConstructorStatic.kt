// FIR_DUMP

import lombok.NoArgsConstructor
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse

@NoArgsConstructor(staticName = "make", force = true)
class ConstructorExampleStatic(val boolean: Boolean, val char: Char, val int: Int, val str: String)

@NoArgsConstructor(staticName = "make", force = true)
class ConstructorExampleStaticWithCompanion(val any: Any) {
    companion object {
        fun Int.make(): Int = 42 // It shouldn't conflict with the generated `make` function
    }
}

@NoArgsConstructor(staticName = "make", force = true)
class ConstructorExampleStaticWithGenerics<T>(val param: T)

@NoArgsConstructor(staticName = "make", force = true)
class ConstructorExampleStaticWithBoundedGenerics<T : Comparable<T>>(val param: T)

@NoArgsConstructor(staticName = "make", force = true)
class ConstructorExampleStaticWithMemberOfTheSameName(val any: Any) {
    // Nothing is generated: this function would shadow the `make` factory at every unqualified call site, so neither
    // the factory, nor the companion object holding it, nor the no-args constructor it would have called appear.
    fun make(): String = "member"

    fun callUnqualified(): String = make()
}

fun box(): String {
    val zeroObject = ConstructorExampleStatic.make()
    assertFalse(zeroObject.boolean)
    assertEquals(Char(0), zeroObject.char)
    assertEquals(0, zeroObject.int)
    assertNull(zeroObject.str)
    val zeroObject2 = ConstructorExampleStaticWithCompanion.make()
    assertNull(zeroObject2.any)
    val zeroObjectWithGenerics = ConstructorExampleStaticWithGenerics.make<String>()
    assertNull(zeroObjectWithGenerics.param)
    val zeroObjectWithBoundedGenerics = ConstructorExampleStaticWithBoundedGenerics.make<Int>()
    assertNull(zeroObjectWithBoundedGenerics.param)
    val withMemberOfTheSameName = ConstructorExampleStaticWithMemberOfTheSameName("any")
    assertEquals("member", withMemberOfTheSameName.callUnqualified())

    return "OK"
}
