import kotlin.test.*
import objcTests.*

/// KT-37067: attribute prefixed with "new" causes cinterop failure
// No matter what is in the test: I only need to get it compilable with cinterop

@Test fun testKT37067() {
    val impl = KT37067_Impl()
    assertEquals(null, impl.newValue)
}
