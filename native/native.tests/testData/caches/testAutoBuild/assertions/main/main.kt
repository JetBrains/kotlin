import kotlin.test.*

@Test
fun runTest() {
    val error = assertFailsWith<AssertionError> { failingAssert() }
    assertEquals("KT89892AssertionMarker", error.message)
}
