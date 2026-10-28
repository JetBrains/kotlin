import kotlin.test.*

@Test
fun runTest() {
    assertEquals(107, leaf())
    assertEquals(1002, shortLeaf())
    assertEquals(11, caller())
    assertEquals(40, unrelated2())
}
