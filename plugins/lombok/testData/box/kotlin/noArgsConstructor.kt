// FIR_DUMP
// FILE: ConstructorExample.kt

import lombok.NoArgsConstructor

@NoArgsConstructor
open class ConstructorExample(val boolean: Boolean, val char: Char, val int: Int, val str: String)

@NoArgsConstructor
class ConstructorExampleWithGenerics<T>(val param: T)

fun box(): String {
    val zeroObject = ConstructorExample()
    assertEquals(false, zeroObject.boolean)
    assertEquals(Char(0), zeroObject.char)
    assertEquals(0, zeroObject.int)
    assertEquals(null, zeroObject.str)

    val zeroObjectWithGenerics = ConstructorExampleWithGenerics<Int>()
    assertEquals(null, zeroObjectWithGenerics.param)

    return "OK"
}
