// EXPECTED_REACHABLE_NODES: 1227
package foo


open class A

open class B : A()

open class C

open class D : B()

class E : D()

class F : C()

val dyn: dynamic = js("({})")

fun nullToNull(a: Any?) = a is B?
fun notNullToNull(a: Any) = a is B?
fun nullToNotNull(a: Any?) = a is B
fun notNullToNotNull(a: Any) = a is B

fun NBtoNB(b: B?) = b is B?
fun dtoNB(d: dynamic) = d is B?

fun testClassCastN2N(): String { // ? is ?
    assertEquals(true, nullToNull(null), "null is B?")
    assertEquals(false, nullToNull(A()), "A? is B?")
    assertEquals(true, NBtoNB(B()), "B()? is B?")
    assertEquals(true, NBtoNB(null), "null() is B?")
    assertEquals(true, nullToNull(B()), "B? is B?")
    assertEquals(false, nullToNull(C()), "C? is B?")
    assertEquals(true, nullToNull(D()), "D? is B?")
    assertEquals(true, nullToNull(E()), "E? is B?")
    assertEquals(false, nullToNull(F()), "F? is B?")
    assertEquals(false, nullToNull(Any()), "Any? is B?")
    assertEquals(true, dtoNB(null), "null dynamic is B?")

    assertEquals(false, nullToNull({}), "Function? is B?")
    assertEquals(false, nullToNull(true), "Boolean? is B?")
    assertEquals(false, nullToNull(42), "Number? is B?")
    assertEquals(false, nullToNull("String"), "String? is B?")

    return "OK"
}

fun NBtoB(b: B?) = b is B

fun testClassCastNN2N(): String { // ! is ?
    assertEquals(false, notNullToNull(A()), "A is B?")
    assertEquals(true, NBtoB(B()), "B() is B?")
    assertEquals(false, NBtoB(null), "null() is B?")
    assertEquals(true, notNullToNull(B()), "B is B?")
    assertEquals(false, notNullToNull(C()), "C is B?")
    assertEquals(true, notNullToNull(D()), "D is B?")
    assertEquals(true, notNullToNull(E()), "E is B?")
    assertEquals(false, notNullToNull(F()), "F is B?")
    assertEquals(false, notNullToNull(Any()), "Any is B?")
    assertEquals(false, dtoNB(dyn), "dynamic is B?")

    assertEquals(false, notNullToNull({}), "Function is B?")
    assertEquals(false, notNullToNull(true), "Boolean is B?")
    assertEquals(false, notNullToNull(42), "Number is B?")
    assertEquals(false, notNullToNull("String"), "String is B?")

    return "OK"
}

fun BtoNB(b: B) = b is B?

fun anyNToAnyNN(a: Any?) = a is Any

fun dtoB(d: dynamic) = d is B

fun testClassCastN2NN(): String { // ? is !
    assertEquals(false, nullToNotNull(null), "null is B")
    assertEquals(false, nullToNotNull(A()), "A? is B")
    assertEquals(true, BtoNB(B()), "B() is B?")
    assertEquals(true, nullToNotNull(B()), "B? is B")
    assertEquals(false, nullToNotNull(C()), "C? is B")
    assertEquals(true, nullToNotNull(D()), "D? is B")
    assertEquals(true, nullToNotNull(E()), "E? is B")
    assertEquals(false, nullToNotNull(F()), "F? is B")
    assertEquals(false, nullToNotNull(Any()), "Any? is B")
    assertEquals(false, dtoB(null), "null dynamic is B")
    assertEquals(true, anyNToAnyNN(Any()), "Any? is Any")
    assertEquals(false, anyNToAnyNN(null), "null any is Any")

    assertEquals(false, nullToNotNull({}), "Function? is B")
    assertEquals(false, nullToNotNull(true), "Boolean? is B")
    assertEquals(false, nullToNotNull(42), "Number? is B")
    assertEquals(false, nullToNotNull("String"), "String? is B")

    assertEquals(true, anyNToAnyNN({}), "Function? is Any")
    assertEquals(true, anyNToAnyNN(true), "Boolean? is Any")
    assertEquals(true, anyNToAnyNN(42), "Number? is Any")
    assertEquals(true, anyNToAnyNN("String"), "String? is Any")

    return "OK"
}

fun testClassCastNN2NN(): String { // ! is !
    assertEquals(false, notNullToNotNull(A()), "A is B")
    assertEquals(true, notNullToNotNull(B()), "B is B")
    assertEquals(false, notNullToNotNull(C()), "C is B")
    assertEquals(true, notNullToNotNull(D()), "D is B")
    assertEquals(true, notNullToNotNull(E()), "E is B")
    assertEquals(false, notNullToNotNull(F()), "F is B")
    assertEquals(false, notNullToNotNull(Any()), "Any is B")
    assertEquals(false, dtoB(dyn), "dynamic is B")

    assertEquals(false, notNullToNotNull({}), "Function is II")
    assertEquals(false, notNullToNotNull(true), "Boolean is II")
    assertEquals(false, notNullToNotNull(42), "Number is II")
    assertEquals(false, notNullToNotNull("String"), "String is II")

    return "OK"
}

fun testClassCast() {
    assertEquals("OK", testClassCastN2N())
    assertEquals("OK", testClassCastN2NN())
    assertEquals("OK", testClassCastNN2N())
    assertEquals("OK", testClassCastNN2NN())
}

fun box(): String {
    testClassCast()
    return "OK"
}