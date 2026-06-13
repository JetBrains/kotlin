// FIR_DUMP
// FILE: ConstructorExample.kt

import lombok.NoArgsConstructor

@NoArgsConstructor
open class ConstructorExample(var boolean: Boolean, var char: Char, var int: Int, var str: String)

@NoArgsConstructor
class ConstructorExampleWithGenerics<T>(var param: T)

@NoArgsConstructor(force = true)
class ConstructorExampleWithForce(val int: Int) {
    // The following properties should not affect generation
    val x = 5
    val y: String
        get() = "y"
    val z by lazy { "TEST" }
}

fun box(): String {
    val zeroObject = ConstructorExample()
    assertEquals(false, zeroObject.boolean)
    assertEquals(Char(0), zeroObject.char)
    assertEquals(0, zeroObject.int)
    assertEquals(null, zeroObject.str)

    val zeroObjectWithGenerics = ConstructorExampleWithGenerics<Int>()
    assertEquals(null, zeroObjectWithGenerics.param)

    assertEquals(0, ConstructorExampleWithForce().int)

    return "OK"
}
