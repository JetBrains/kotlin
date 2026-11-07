import kotlin.test.*

@Test
fun runTest() {
    // S no longer implements Marker: both upcasts must fail for a value of type S. A
    // stale IC keeps check.kt's bitcode with both casts erased and fails both assertions.
    val s = S()
    assertEquals(false, safeUp(s))
    assertEquals("cce", hardUp(s))
}
