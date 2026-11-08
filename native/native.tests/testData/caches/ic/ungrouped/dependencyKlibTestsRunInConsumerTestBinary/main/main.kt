import kotlin.test.*

@Test
fun runTest() {
    // The library's ordinary API works in every build. A correct IC registers only this one
    // test and the binary passes. Today's IC also registers lib's libTest: the unconditional
    // TestsInitializer lowering bakes the library's registration into lib's per-file cache,
    // so the from-scratch IC binary runs 2 tests and libTest fails it, while a non-IC build
    // of the same klibs runs 1 test and passes.
    assertEquals(7, libValue())
}
