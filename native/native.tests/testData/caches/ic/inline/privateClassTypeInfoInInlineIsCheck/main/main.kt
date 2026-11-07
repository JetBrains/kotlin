import kotlin.test.*

@Test
fun runTest() {
    // "s" is not a Hidden: a fixed IC answers false. The IC link fails before this runs
    // (undefined "_kclass:<...>/src/lib1/g.kt.Hidden" referenced from main's per-file
    // object); the same klibs link without incremental compilation.
    assertEquals(false, g("s"))
}
