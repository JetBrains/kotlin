import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals("b:1:7", viaByte(ByteImpl()))
    assertEquals("l:2:9", viaLong(LongImpl()))
    assertEquals("r:3:s", viaRef(RefImpl()))
    assertEquals("m:4:5", viaMid(ByteImpl()))
}
