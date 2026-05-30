// FIR_DUMP

import lombok.NoArgsConstructor

@NoArgsConstructor(staticName = "make")
class ConstructorExampleStatic(val boolean: Boolean, val char: Char, val int: Int, val str: String)

@NoArgsConstructor(staticName = "make")
class ConstructorExampleStaticWithCompanion(val any: Any) {
    companion object {
        fun Int.make(): Int = 42 // It shouldn't conflict with the generated `make` function
    }
}

fun box(): String {
    val zeroObject = ConstructorExampleStatic.make()
    assertEquals(false, zeroObject.boolean)
    assertEquals(Char(0), zeroObject.char)
    assertEquals(0, zeroObject.int)
    assertEquals(null, zeroObject.str)

    val zeroObject2 = ConstructorExampleStaticWithCompanion.make()
    assertEquals(null, zeroObject2.any)

    return "OK"
}
