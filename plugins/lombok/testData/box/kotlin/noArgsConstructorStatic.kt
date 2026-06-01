// FIR_DUMP

import lombok.NoArgsConstructor

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

fun box(): String {
    val zeroObject = ConstructorExampleStatic.make()
    assertEquals(false, zeroObject.boolean)
    assertEquals(Char(0), zeroObject.char)
    assertEquals(0, zeroObject.int)
    assertEquals(null, zeroObject.str)

    val zeroObject2 = ConstructorExampleStaticWithCompanion.make()
    assertEquals(null, zeroObject2.any)

    val zeroObjectWithGenerics = ConstructorExampleStaticWithGenerics.make()
    assertEquals(null, zeroObjectWithGenerics.param)

    return "OK"
}
