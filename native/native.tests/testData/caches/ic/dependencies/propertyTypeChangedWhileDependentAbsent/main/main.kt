import kotlin.test.*

@Test
fun runTest() {
    // A correct IC rebuilds x's a.kt when x returns to the link (p's declared type changed
    // while x was absent) and answers "s". The IC link fails before this runs
    // (undefined "_kfun:#<get-p>(){}kotlin.String" referenced from _kfun:#a(){} in x's stale
    // per-file archive); the clean build recompiles a.kt and links.
    assertEquals("s", a())
}
