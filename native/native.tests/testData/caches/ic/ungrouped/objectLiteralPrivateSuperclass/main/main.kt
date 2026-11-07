import kotlin.test.*

@Test
fun runTest() {
    // A fixed IC links and answers "ok". The IC build crashes before this runs: main (a
    // friend of lib1) re-creates the object literal over the private superclass P and must
    // read P's class_fields from lib1's per-file cache, whose FileLocalSignature key no
    // consumer session can match ("No class fields for CLASS name:P visibility:private").
    // The same klibs link without incremental compilation.
    assertEquals("ok", f())
}
