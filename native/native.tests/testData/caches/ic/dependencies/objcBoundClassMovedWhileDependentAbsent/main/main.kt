import kotlin.test.*
import y.Local

@Test
fun runTest() {
    // A correct IC rebuilds x's cache when x returns to the link (Bound moved to y2.kt while
    // x was absent), the link succeeds and the test passes at every step. The step-2 IC link
    // fails before this runs: x's stale per-file cache still pulls only y.kt and replays an
    // ObjC adapter that imports "_kclass:y.Bound", which no linked archive defines anymore.
    // The main module deliberately does not reference y.Bound: referencing the moved class
    // would pull y2.kt into the link and mask the missing invalidation.
    assertEquals("Local", Local::class.simpleName)
}
