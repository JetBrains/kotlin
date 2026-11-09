import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(5_000_000_007L, readV(pass(makeV())))
    assertEquals<Any>(makeV(), passAny(makeV()))
    assertEquals(5_000_000_007L, readV(makeSub().v))
}
