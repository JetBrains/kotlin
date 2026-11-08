import kotlin.test.*
import lib1.E

@Test
fun runTest() {
    // lib2 is out of the link at this step; the enum it will be re-linked against has grown
    // to three entries while it was away.
    assertEquals(3, E.entries.size)
}
