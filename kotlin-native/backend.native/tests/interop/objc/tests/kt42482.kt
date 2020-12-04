import kotlin.native.ref.WeakReference
import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test
fun testKT42482() {
    // Attempt to make the state predictable:
    kotlin.native.internal.GC.collect()

    kt42482Deallocated = false
    assertFalse(kt42482Deallocated);

    {
        assertEquals(41, KT42482().fortyTwo())

        val obj: KT42482 = KT42482Impl()
        assertEquals(42, obj.fortyTwo())

        kt42482Swizzle(obj)
        assertEquals(43, obj.fortyTwo())

        // Test retain and release on swizzled object:
        kt42482Global = obj
        kt42482Global = null
    }()

    kotlin.native.internal.GC.collect()

    assertTrue(kt42482Deallocated)
}

class KT42482Impl : KT42482() {
    override fun fortyTwo() = 42
}
