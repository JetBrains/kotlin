import kotlin.test.*

@Test
fun runTest() {
    assertFailsWith<Error> { f(object : A {}) }
}
