import kotlin.test.*

@Test
fun runTest() {
    // The external library's ordinary API works in every build. A correct IC registers only
    // this one test and the binary passes. Today's IC also registers externalLib's extTest:
    // the unconditional TestsInitializer lowering bakes the registration into the external
    // module's monolithic auto-cache, so every IC test binary runs 2 tests and extTest fails
    // it, while a non-IC build of the same klibs runs 1 test and passes.
    assertEquals(9, extValue())
}
