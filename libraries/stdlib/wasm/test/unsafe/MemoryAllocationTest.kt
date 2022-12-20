package test.wasm.unsafe

import kotlin.wasm.unsafe.*
import kotlin.test.*

class MemoryAllocationTest {
    @Test
    fun testWasmMemorySizeGrow() {
        val s1 = wasmMemorySize()
        val grow_res = wasmMemoryGrow(10)
        val s2 = wasmMemorySize()
        assertNotEquals(grow_res, -1)
        assertEquals(grow_res, s1)
        assertEquals(s2 - s1, 10)
        println(s1)
        println(s2)
        println(grow_res)
    }
}