import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(7L, readV(pass(makeV())))
    assertEquals<Any>(makeV(), passAny(makeV()))
    assertEquals(7L, readV(makeSub().v))
}
