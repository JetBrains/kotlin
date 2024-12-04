import kotlin.test.*

private val x = foo()

private fun foo(): Int {
    z2 = true
    return 42
}

@Test
fun test2() {
    t2 = true
    assertTrue(z2)
    assertTrue(t1 || !z1)
}