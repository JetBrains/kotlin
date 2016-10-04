package foo

open class A

class B : A() {
    val a = 1
}

object O

interface I

enum class E {
    X,
    Y {
        val a = 1
    },
    Z {}
}

@JsName("Q")
class R

fun check(x: Any, y: Any, shouldBeEqual: Boolean = true, shouldBeSame: Boolean = true) {
    assertNotEquals(null, x)
    assertNotEquals(null, y)
    if (shouldBeEqual) {
        assertEquals(x, y)

        if (shouldBeSame && x !== y) {
            fail("Expected same instances, got expected = '$x', actual = '$y'")
        }
    }
    else {
        assertNotEquals(x, y)
    }
}
