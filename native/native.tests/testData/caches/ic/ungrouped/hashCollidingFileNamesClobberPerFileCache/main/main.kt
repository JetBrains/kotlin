import kotlin.test.*

@Test
fun runTest() {
    // A correct IC gives the two colliding files one per-file cache directory each, so f1's
    // archive exists, the link succeeds and the test passes. Today's IC fails the link before
    // this runs: Aa.kt's cache is clobbered by BB.kt's (both map to the same cacheFileId), so
    // the link reports undefined "_kfun:#f1(){}kotlin.Int" and an undefined uniquified
    // "__Konan_init_main:ROOT.<hash>" ctor symbol. The non-IC build of the same klib links
    // and answers 7.
    assertEquals(7, f1())
}
