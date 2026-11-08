import kotlin.test.*

@Test
fun runTest() {
    // Nothing needs to be called: the IC pipeline builds a per-file cache for every klib of
    // the link, and the cache build of lib2's b.kt alone crashes with
    // "IrPropertySymbolImpl is already bound. Signature: p/vf|{}vf[0]" before this test could
    // run. A correct IC tolerates (or rejects upfront) the same-signature twin reachable from
    // the dependency's cached inline body, builds the caches, links and runs this test -- as
    // a non-IC build of the same klibs does today.
}
