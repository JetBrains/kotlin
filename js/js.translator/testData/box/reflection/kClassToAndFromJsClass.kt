// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1463
package foo

import kotlin.reflect.KClass

fun check(k: KClass<*>, j: JsClass<*>) {
    assertNotEquals(null, k)
    assertNotEquals(null, j)

    assertSame(k.js, j)
    assertSame(k, j.kotlin)
    assertSame(j, j.kotlin.js)
    assertSame(k, k.js.kotlin)
}

fun jsClassbyName(name: String) = js("main").foo[name]

fun box(): String {
    check(A::class, jsClassbyName("A"))
    check(B::class, jsClassbyName("B"))
    check(O::class, jsClassbyName("O").constructor)
    check(I::class, jsClassbyName("I"))
    check(R::class, jsClassbyName("Q"))
    check(E::class, jsClassbyName("E"))
    check(E.X::class, jsClassbyName("E").X.constructor)
    check(E.Y::class, jsClassbyName("E").Y.constructor)
// TODO uncomment after KT-13338 is fixed
//    check(E.Z::class, jsClassbyName("E").Z.constructor)

    return "OK"
}
