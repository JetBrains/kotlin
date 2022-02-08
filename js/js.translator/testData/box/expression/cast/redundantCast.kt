// EXPECTED_REACHABLE_NODES: 1306

// Test that upcasts are optimized away

open class Base

class Derived: Base()

var counter = 0
var _derived = Derived()

@JsExport
fun getDerived(): Derived {
    counter += 1
    return _derived
}

@JsExport
fun getDerivedNullable(): Derived? {
    counter += 1
    return _derived
}

@JsExport
fun getDerivedNull(): Derived? {
    counter += 1
    return null
}

// CHECK_CONTAINS_NO_CALLS: upcast1 except=getDerived IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=upcast1 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=upcast1 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=upcast1 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun upcast1() = getDerived() as Base

// CHECK_CONTAINS_NO_CALLS: upcast2 except=getDerived IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=upcast2 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=upcast2 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=upcast2 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun upcast2() = getDerived() as Base?

// CHECK_BINOP_COUNT: function=upcast3 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun upcast3() = getDerivedNullable() as Base

// CHECK_CONTAINS_NO_CALLS: upcast4 except=getDerivedNullable IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=upcast4 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=upcast4 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=upcast1 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun upcast4() = getDerivedNullable() as Base?

// CHECK_BINOP_COUNT: function=upcast5 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun upcast5() = getDerivedNull() as Base

// CHECK_CONTAINS_NO_CALLS: upcast6 except=getDerivedNull IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=upcast6 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=upcast6 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=upcast6 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun upcast6() = getDerivedNull() as Base?

// CHECK_CONTAINS_NO_CALLS: safeCast1 except=getDerived IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=safeCast1 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=safeCast1 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=safeCast1 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun safeCast1() = getDerived() as? Base

// CHECK_CONTAINS_NO_CALLS: safeCast2 except=getDerived IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=safeCast1 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=safeCast1 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=safeCast2 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun safeCast2() = getDerived() as? Base?

// CHECK_BINOP_COUNT: function=safeCast3 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun safeCast3() = getDerivedNullable() as? Base

// CHECK_CONTAINS_NO_CALLS: safeCast4 except=getDerivedNullable IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=safeCast4 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=safeCast4 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=safeCast4 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun safeCast4() = getDerivedNullable() as? Base?

// CHECK_BINOP_COUNT: function=safeCast5 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun safeCast5() = getDerivedNull() as? Base

// CHECK_CONTAINS_NO_CALLS: safeCast6 except=getDerivedNull IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=safeCast6 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=safeCast6 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=safeCast6 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun safeCast6() = getDerivedNull() as? Base?

// CHECK_CONTAINS_NO_CALLS: upcast1 except=getDerived IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=upcast1 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=upcast1 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=instanceCheck1 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun instanceCheck1() = getDerived() is Base

// CHECK_CONTAINS_NO_CALLS: upcast2 except=getDerived IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=upcast2 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=upcast2 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=instanceCheck2 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun instanceCheck2() = getDerived() is Base?

// CHECK_BINOP_COUNT: function=instanceCheck3 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun instanceCheck3() = getDerivedNullable() is Base

// CHECK_CONTAINS_NO_CALLS: upcast4 except=getDerivedNullable IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=upcast4 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=upcast4 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=instanceCheck4 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun instanceCheck4() = getDerivedNullable() is Base?

// CHECK_BINOP_COUNT: function=instanceCheck5 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun instanceCheck5() = getDerivedNull() is Base

// CHECK_CONTAINS_NO_CALLS: upcast6 except=getDerivedNull IGNORED_BACKENDS=JS
// CHECK_TERNARY_OPERATOR_COUNT: function=upcast6 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=upcast6 count=0 IGNORED_BACKENDS=JS
// CHECK_BINOP_COUNT: function=instanceCheck6 count=0 symbol=instanceof IGNORED_BACKENDS=JS
fun instanceCheck6() = getDerivedNull() is Base?

fun box(): String {
    assertSame(_derived, upcast1(), "upcast1()")
    assertSame(_derived, upcast2(), "upcast2()")
    assertSame(_derived, upcast3(), "upcast3()")
    assertSame(_derived, upcast4(), "upcast4()")
    failsClassCast("upcast5()") { upcast5() }
    assertSame(null, upcast6(), "upcast6()")
    assertEquals(6, counter)

    counter = 0

    assertSame(_derived, safeCast1(), "safeCast1()")
    assertSame(_derived, safeCast2(), "safeCast2()")
    assertSame(_derived, safeCast3(), "safeCast3()")
    assertSame(_derived, safeCast4(), "safeCast4()")
    assertSame(null,     safeCast5(), "safeCast5()")
    assertSame(null,     safeCast6(), "safeCast6()")
    assertEquals(6, counter)

    counter = 0

    assertTrue(instanceCheck1(), "instanceCheck1()")
    assertTrue(instanceCheck2(), "instanceCheck2()")
    assertTrue(instanceCheck3(), "instanceCheck3()")
    assertTrue(instanceCheck4(), "instanceCheck4()")
    assertFalse(instanceCheck5(), "instanceCheck5()")
    assertTrue(instanceCheck6(), "instanceCheck6()")
    assertEquals(6, counter)

    return "OK"
}
