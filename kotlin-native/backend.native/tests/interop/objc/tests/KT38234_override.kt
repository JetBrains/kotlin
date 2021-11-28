import kotlin.test.*
import objcTests.*

class KT38234_Impl : KT38234_P1Protocol, KT38234_Base() {
    override fun foo(): Int = 566
}

@Test fun testKT38234() {
    assertEquals(566, KT38234_Impl().callFoo())
}
