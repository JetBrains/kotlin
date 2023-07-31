package test.wasm.unsafe

import kotlin.wasm.unsafe.*
import kotlin.test.*

private fun jsConcatStrings(a: String, b: String): String =
    js("a + b")

@OptIn(UnsafeWasmMemoryApi::class)
class MemoryAllocationJsTest {
    @Test
    fun testJsIntropInsideAllocations() {
        withScopedMemoryAllocator { allocator ->
            assertEquals(jsConcatStrings("str1", "str2"), "str1str2")
            allocator.allocate(10)
        }
    }
}