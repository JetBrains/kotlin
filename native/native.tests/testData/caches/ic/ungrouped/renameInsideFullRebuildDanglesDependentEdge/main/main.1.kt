import kotlin.test.*
import test.*

@Test
fun runTest() {
    // The edit keeps main dirty, so the link rebuilds main's per-file cache; resolving
    // main.kt -> lib2|use.kt -> lib1|q.kt (the removed file id) crashes the IC link before
    // this can run. The clean link answers 7, 7.
    assertEquals(7, pub())
    assertEquals(7, viaPub())
}
