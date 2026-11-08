import kotlin.test.*
import p.Q

@Test
fun runTest() {
    // A correct IC emits the cached adapter's reverse-bridge impl with a linkage the replayed
    // import can resolve, links and answers 7. Today's IC fails the link before this runs:
    // "Undefined symbols ... _kfun:<...>/src/lib1/q.kt.p#tagRev(p.Q){}kotlin.Int, referenced
    // from ___unnamed_4" - the adapter replayed from lib1's ir/objc_adapters imports the
    // impl as an extern, while the per-file archive defines it as a LOCAL symbol. The non-IC
    // build links and answers 7.
    assertEquals(7, Q().tag())
}
