package foo

import kotlin.reflect.KClass

fun check(k: KClass<*>, j: JsClass<*>) {
    assertNotEquals(null, k)
    assertNotEquals(null, j)

    assertEquals(k.js, j)
    assertEquals(k, j.kotlin)
    assertEquals(j, j.kotlin.js)
    assertEquals(k, k.js.kotlin)
}

fun jsClassbyName(name: String) = js("JS_TESTS").foo[name]

fun box(): String {
    check(A::class, jsClassbyName("A"))
    check(B::class, jsClassbyName("B"))
    check(O::class, jsClassbyName("O").constructor)
    check(I::class, jsClassbyName("I"))
    check(E::class, jsClassbyName("E"))
    check(E.X::class, jsClassbyName("E").X.constructor)
    check(E.Y::class, jsClassbyName("E").Y.constructor)
// TODO uncomment after KT-13338 is fixed
//    check(E.Z::class, jsClassbyName("E").Z.constructor)

    return "OK"
}
