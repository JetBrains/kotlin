// EXPECTED_REACHABLE_NODES: 1234
package foo


open class A
open class B : A() // B
open class C
open class D : B() // B
class E : D() // B
class F : C()

val dyn: dynamic = js("({})")

fun nullToNull(aa: Any?) = aa as? B?
fun notNullToNull(aa: Any) = aa as? B?
fun nullToNotNull(aa: Any?) = aa as? B
fun notNullToNotNull(aa: Any) = aa as? B

fun NBtoNB(bb: B?) = bb as? B?
fun dtoNB(dd: dynamic) = dd as? B?

val an = Any()

val a = A()
val b = B()
val c = C()
val d = D()
val e = E()
val f = F()

fun fff() {}

val func = ::fff
val bool = true
val num = 42
val str = "String"

fun testClassCastN2N(): String { // ? as? ?
    assertEquals(null, nullToNull(null), "null as? B?")
    assertEquals(null, nullToNull(a), "A? as? B?")
    assertEquals(b, NBtoNB(b), "b? as? B?")
    assertEquals(null, NBtoNB(null), "null() as? B?")
    assertEquals(b, nullToNull(b), "B? as? B?")
    assertEquals(null, nullToNull(c), "C? as? B?")
    assertEquals(d, nullToNull(d), "D? as? B?")
    assertEquals(e, nullToNull(e), "E? as? B?")
    assertEquals(null, nullToNull(f), "F? as? B?")
    assertEquals(null, nullToNull(an), "Any? as? B?")
    assertEquals(null, dtoNB(null), "null dynamic as? B?")

    assertEquals(null, nullToNull(func), "Function? is B?")
    assertEquals(null, nullToNull(bool), "Boolean? is B?")
    assertEquals(null, nullToNull(num), "Number? is B?")
    assertEquals(null, nullToNull(str), "String? is B?")

    return "OK"
}

fun NBtoB(bb: B?) = bb as? B

fun testClassCastNN2N(): String { // ! as? ?
    assertEquals(null, notNullToNull(a), "A as? B?")
    assertEquals(b, NBtoB(b), "b as? B?")
    assertEquals(null, NBtoB(null), "null() as? B?")
    assertEquals(b, notNullToNull(b), "B as? B?")
    assertEquals(null, notNullToNull(c), "C as? B?")
    assertEquals(d, notNullToNull(d), "D as? B?")
    assertEquals(e, notNullToNull(e), "E as? B?")
    assertEquals(null, notNullToNull(f), "F as? B?")
    assertEquals(null, notNullToNull(an), "Any as? B?")
    assertEquals(null, dtoNB(dyn), "dynamic as? B?")

    assertEquals(null, notNullToNull(func), "Function is B?")
    assertEquals(null, notNullToNull(bool), "Boolean is B?")
    assertEquals(null, notNullToNull(num), "Number is B?")
    assertEquals(null, notNullToNull(str), "String is B?")

    return "OK"
}

fun BtoNB(bb: B) = bb as? B?

fun anyNToAnyNN(aa: Any?) = aa as? Any

fun dtoB(dd: dynamic) = dd as? B

fun testClassCastN2NN(): String { // ? as? !
    assertEquals(null, nullToNotNull(null), "null as? B")
    assertEquals(null, nullToNotNull(a), "A? as? B")
    assertEquals(b, BtoNB(b), "b as? B?")
    assertEquals(b, nullToNotNull(b), "B? as? B")
    assertEquals(null, nullToNotNull(c), "C? as? B")
    assertEquals(d, nullToNotNull(d), "D? as? B")
    assertEquals(e, nullToNotNull(e), "E? as? B")
    assertEquals(null, nullToNotNull(f), "F? as? B")
    assertEquals(null, nullToNotNull(Any()), "Any? as? B")
    assertEquals(null, dtoB(null), "null dynamic as? B")
    assertEquals(an, anyNToAnyNN(an), "Any? as? Any")
    assertEquals(null, anyNToAnyNN(null), "null any as? Any")

    assertEquals(null, nullToNotNull(func), "Function? is B")
    assertEquals(null, nullToNotNull(bool), "Boolean? is B")
    assertEquals(null, nullToNotNull(num), "Number? is B")
    assertEquals(null, nullToNotNull(str), "String? is B")

    assertEquals(func, anyNToAnyNN(func), "Function? is Any")
    assertEquals(bool, anyNToAnyNN(bool), "Boolean? is Any")
    assertEquals(num, anyNToAnyNN(num), "Number? is Any")
    assertEquals(str, anyNToAnyNN(str), "String? is Any")

    return "OK"
}

fun testClassCastNN2NN(): String { // ! as? !
    assertEquals(null, notNullToNotNull(a), "A as? B")
    assertEquals(b, notNullToNotNull(b), "B as? B")
    assertEquals(null, notNullToNotNull(c), "C as? B")
    assertEquals(d, notNullToNotNull(d), "D as? B")
    assertEquals(e, notNullToNotNull(e), "E as? B")
    assertEquals(null, notNullToNotNull(f), "F as? B")
    assertEquals(null, notNullToNotNull(an), "Any as? B")
    assertEquals(null, dtoB(dyn), "dynamic as? B")

    assertEquals(null, notNullToNotNull(func), "Function is II")
    assertEquals(null, notNullToNotNull(bool), "Boolean is II")
    assertEquals(null, notNullToNotNull(num), "Number is II")
    assertEquals(null, notNullToNotNull(str), "String is II")

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