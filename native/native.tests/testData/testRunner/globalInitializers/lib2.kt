import kotlin.test.*

private val x = foo()

private fun foo(): Int {
    z1 = true
    return 42
}

@Test
fun test1() {
    t1 = true
    assertTrue(z1)
    assertTrue(t2 || !z2)
}