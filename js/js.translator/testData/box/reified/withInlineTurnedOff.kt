package foo

// NO_INLINE

class A
class B

fun box(): String {
    val a = A()
    val b = B()

    assertEquals(true, isInstance<A>(a), "isInstance<A>(a)")
    assertEquals(false, isInstance<A>(b), "isInstance<A>(b)")

    assertEquals(true, isInstance<A?>(a), "isInstance<A?>(a)")
    assertEquals(true, isInstance<A?>(null), "isInstance<A?>(null)")
    assertEquals(false, isInstance<A?>(b), "isInstance<A?>(b)")

    return "OK"
}