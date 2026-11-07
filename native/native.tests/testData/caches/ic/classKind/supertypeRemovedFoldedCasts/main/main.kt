import kotlin.test.*

@Test
fun runTest() {
    val s = S()
    assertEquals(true, safeUp(s))
    assertEquals("cast-ok", hardUp(s))
}
