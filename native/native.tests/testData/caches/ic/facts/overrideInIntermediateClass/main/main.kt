import kotlin.test.*

@Test
fun runTest() {
    assertEquals(11, call() + unrelated())
}
