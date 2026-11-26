import kotlin.test.*
import lib1.E
import lib2.describe

@Test
fun runTest() {
    // E.B must dispatch to the E.B branch in every build (the clean and non-IC links
    // re-lower lib2's when against the current enum and pass). Today the step-2 warm IC
    // links reuse lib2's stale step-0 cache, whose ordinal switch maps E.B's new ordinal 1
    // into the C branch: Expected <b>, actual <c>.
    assertEquals("b", describe(E.B))
}
