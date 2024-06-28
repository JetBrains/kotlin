package test.wasm.unsafe

import kotlin.wasm.unsafe.*
import kotlin.test.*

@OptIn(UnsafeWasmMemoryApi::class)
class MemoryAllocationTest {
    val pageSize = 65_536

    @Test
    fun testWasmMemorySizeGrow() {
        val s1 = wasmMemorySize()
        val grow_res = wasmMemoryGrow(10)
        val s2 = wasmMemorySize()
        assertNotEquals(grow_res, -1)
        assertEquals(grow_res, s1)
        assertEquals(s2 - s1, 10)
    }

    @Test
    fun testScopedAllocator() {
        val sizes = listOf<Int>(1, 1, 2, 3, 8, 10, 305, 12_747, 31_999)
        val allocations = mutableListOf<Pointer>()
        withScopedMemoryAllocator { a ->
            for (size in sizes) {
                allocations += a.allocate(size)
            }
        }

        val allocations2 = mutableListOf<Pointer>()
        withScopedMemoryAllocator { a ->
            for (size in sizes) {
                allocations2 += a.allocate(size)
            }
        }

        // Test that we run withScopedMemoryAllocator body
        assertEquals(allocations.size, sizes.size)

        // Memory should be reusing in different scopes
        // NOTE: This is current impl detail and can be changed
        assertEquals(allocations, allocations2)

        // Allocations are aligned
        assertTrue(allocations.all { it.address % 8u == 0u })

        // Allocations are different
        assertTrue(allocations.distinct().size == allocations.size)

        // Allocations do not intersect
        for (i1 in 0..<sizes.size) {
            for (i2 in (i1 + 1)..<sizes.size) {
                val a1 = allocations[i1].address
                val a2 = allocations[i2].address
                val size1 = sizes[i1].toUInt()
                val size2 = sizes[i2].toUInt()
                assertTrue(a1 !in a2..<a2 + size2)
                assertTrue(a1 + size1 - 1u !in a2..<a2 + size2)
            }
        }
    }

    @Test
    fun testScopedAllocatorGrowsMemory() {
        // Allocations past current memory size should grow memory
        val memSizes = mutableListOf<Int>(wasmMemorySize())
        withScopedMemoryAllocator { a ->
            var allocatedAddress = a.allocate(pageSize)
            var allocationSize = pageSize

            repeat(10) {
                var currPagesUsed = (allocatedAddress.address.toInt() + allocationSize + 1) / pageSize
                var currPagesAvailable = wasmMemorySize()
                assertTrue(currPagesAvailable > currPagesUsed)
                // Allocate 10 pages past max page
                allocationSize = (currPagesAvailable - currPagesUsed + 10) * pageSize
                allocatedAddress = a.allocate(allocationSize)
                memSizes += wasmMemorySize()
            }
        }
        assertTrue(memSizes.size == 11)
        assertEquals(memSizes.distinct(), memSizes)
        assertEquals(memSizes.sorted(), memSizes)
    }

    @Test
    fun nestedAllocators() {
        val sizes = listOf<Int>(1, 1, 2, 3, 8, 10, 305, 12_747, 31_999)
        var allocations1: List<Pointer>
        var allocations1_1: List<Pointer>
        var allocations1_2: List<Pointer>
        var allocations2: List<Pointer>

        withScopedMemoryAllocator { allocator0 ->
            allocations1 = sizes.map { size -> allocator0.allocate(size) }
            withScopedMemoryAllocator { allocator0_0 ->
                allocations1_1 = sizes.map { size -> allocator0_0.allocate(size) }
            }
            withScopedMemoryAllocator { allocator0_1 ->
                allocations1_2 = sizes.map { size -> allocator0_1.allocate(size) }
            }
        }
        withScopedMemoryAllocator { allocator0 ->
            allocations2 = sizes.map { size -> allocator0.allocate(size) }
        }

        // Check that all allocations happened and distinct
        listOf(
            allocations1,
            allocations1_1,
            allocations1_2,
            allocations2
        ).forEach { allocation ->
            assertEquals(allocation.size, sizes.size)
            assertEquals(allocation.distinct().size, allocation.size)
        }

        // Impl detail: sibling scopes use the same memory
        assertEquals(allocations1, allocations2)
        assertEquals(allocations1_1, allocations1_2)

        // Impl detaiol: allocator in child scope allocates new memory
        val max1: UInt = allocations1.maxOf { it.address }
        val min1_1: UInt = allocations1_1.minOf { it.address }
        assertTrue(max1 < min1_1)
    }

    @Test
    fun testNestedAllocatorThrows() {
        var leakedAllocator1: MemoryAllocator? = null
        var leakedAllocator2: MemoryAllocator? = null
        var leakedAllocator3: MemoryAllocator? = null

        withScopedMemoryAllocator { allocator1 ->
            leakedAllocator1 = allocator1
            allocator1.allocate(100)
            // 2-level nesting
            withScopedMemoryAllocator { allocator2 ->
                leakedAllocator2 = allocator2
                allocator2.allocate(100)
                assertFailsWith<IllegalStateException> {
                    allocator1.allocate(100)
                }
                // 3-level nesting
                withScopedMemoryAllocator { allocator3 ->
                    leakedAllocator3 = allocator3
                    allocator3.allocate(100)
                    assertFailsWith<IllegalStateException> {
                        allocator1.allocate(100)
                    }
                    assertFailsWith<IllegalStateException> {
                        allocator2.allocate(100)
                    }
                    allocator3.allocate(100)
                }
                assertFailsWith<IllegalStateException> {
                    leakedAllocator3?.allocate(100)
                }
                // now it is legal to use allocator1 since we're in its immediate scope
                allocator2.allocate(100)
            }
            assertFailsWith<IllegalStateException> {
                leakedAllocator2?.allocate(100)
            }
            allocator1.allocate(100)
        }

        for (leakedAllocator in listOf(leakedAllocator1, leakedAllocator2, leakedAllocator3)) {
            assertFailsWith<IllegalStateException> {
                leakedAllocator?.allocate(100)
            }
        }
    }

    @Test
    fun testScopedAllocatorThrows() {
        assertFailsWith<IllegalStateException> {
            var leakedAllocator: MemoryAllocator? = null
            withScopedMemoryAllocator { allocator ->
                leakedAllocator = allocator
            }
            leakedAllocator?.allocate(10)
        }

        assertFailsWith<IllegalStateException> {
            var leakedAllocator: MemoryAllocator? = null
            try {
                withScopedMemoryAllocator { allocator ->
                    leakedAllocator = allocator
                    throw Error()
                }
            } catch (e: Throwable) {
            }
            leakedAllocator?.allocate(10)
        }

        assertFailsWith<IllegalStateException> {
            fun foo(): MemoryAllocator {
                withScopedMemoryAllocator { allocator ->
                    return allocator  // non-local return
                }
            }
            foo().allocate(10)
        }
    }
}