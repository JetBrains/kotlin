import kotlin.test.*

@Test
fun runTest() {
    assertEquals(12, call() + unrelated())
}
