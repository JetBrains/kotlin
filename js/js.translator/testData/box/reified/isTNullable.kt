// EXPECTED_REACHABLE_NODES: 495
package foo

// CHECK_NOT_CALLED: isTypeOfOrNull
// CHECK_NULLS_COUNT: function=box count=10

inline
fun <reified T> Any?.isTypeOfOrNull() = this is T?

class A
class B

fun box(): String {
    assertEquals(true, null.isTypeOfOrNull<A>(), "null.isTypeOfOrNull<A>()")
    assertEquals(true, null.isTypeOfOrNull<A?>(), "null.isTypeOfOrNull<A?>()")
    assertEquals(true, A().isTypeOfOrNull<A>(), "A().isTypeOfOrNull<A>()")
    assertEquals(true, A().isTypeOfOrNull<A?>(), "A().isTypeOfOrNull<A?>()")
    assertEquals(false, A().isTypeOfOrNull<B>(), "A().isTypeOfOrNull<B>()")
    assertEquals(false, A().isTypeOfOrNull<B?>(), "A().isTypeOfOrNull<B?>()")

    return "OK"
}
