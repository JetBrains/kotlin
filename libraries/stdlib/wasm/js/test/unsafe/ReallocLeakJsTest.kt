package test.wasm.unsafe

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.*
import kotlin.wasm.unsafe.*

/**
 * Returns the number of bytes currently used on the V8 heap, after forcing a full garbage collection.
 *
 * V8 allocates WasmGC objects on its regular managed heap, so this includes all live Kotlin/Wasm objects.
 *
 * NOTE: gc() is normally only available when Node is started with --expose-gc. To avoid depending on
 *       test-runner flags, the flag is enabled at runtime and gc() is run in a freshly created context.
 */
@OptIn(ExperimentalWasmJsInterop::class)
private fun heapUsedAfterGc(): Double {
    js(
        """
        process.getBuiltinModule('v8').setFlagsFromString('--expose-gc');
        process.getBuiltinModule('vm').runInNewContext('gc')();
        return process.memoryUsage().heapUsed;
        """
    )
}

/**
 * Black-box regression test for KT-90119 and similar issues: `componentModelRealloc` must not retain any WasmGC memory per
 * operation when all allocations are freed again.
 *
 * NOTE: ComponentModelReallocTest.kt and friends test that no *Wasm linear memory* itself is leaked; whereas this test is purely about WasmGC memory.
 */
@OptIn(ComponentModelInternalApi::class, ExperimentalKotlinTestApi::class)
class ReallocLeakJsTest {
    private val iterations = 1_000_000

    // Everything is single-threaded and measured after a forced full GC, so the measurement is nearly exact:
    // observed run-to-run variation of the heap growth is in the range of a few hundred bytes.
    // Anything beyond a few KiB of growth over a million iterations is almost certainly a leak, not noise.
    private val maxAllowedGrowthBytes = 32.0 * 1024

    /** Runs through every `componentModelRealloc` code path, freeing everything again, so the live set stays constant. */
    private fun churnRealloc(iterations: Int) {
        // Also create 'long-lived' allocations alongside
        val blocker = componentModelRealloc(0, 0, 8)
        repeat(iterations) {
            // test a bunch of different scenarios/code paths
            val freshAllocation = componentModelRealloc(0, 0, 24)
            val shrunkInPlace = componentModelRealloc(freshAllocation, 24, 16)
            val grownInPlace = componentModelRealloc(shrunkInPlace, 16, 48)
            val fillerToPreventFurtherInPlaceGrowth = componentModelRealloc(0, 0, 8)
            val notGrownInPlace = componentModelRealloc(grownInPlace, 48, 96)
            assertNotEquals(grownInPlace, notGrownInPlace) { "This allocation must not grow in place" }

            // zero-size allocation and free
            componentModelRealloc(componentModelRealloc(0, 0, 0), 0, 0)

            // free
            componentModelRealloc(fillerToPreventFurtherInPlaceGrowth, 8, 0)
            componentModelRealloc(notGrownInPlace, 96, 0)
        }
        componentModelRealloc(blocker, 8, 0)
    }

    @Test
    fun reallocDoesNotRetainMemoryPerOperation() {
        // warm up to reduce noise
        churnRealloc(iterations / 100)

        val before = heapUsedAfterGc()
        churnRealloc(iterations)
        val after = heapUsedAfterGc()

        val growth = after - before
        assertTrue(growth < maxAllowedGrowthBytes) {
            "componentModelRealloc retained ${growth / 1024} KiB (=${growth / 1024 / 1024} MiB) of heap over $iterations alloc/free iterations"
        }
    }

    /**
     * Control: makes sure the measurement actually detects per-iteration retention of small objects
     * (as would happen with a leaking tracking list), so that [reallocDoesNotRetainMemoryPerOperation] can't pass silently.
     */
    @Test
    fun heapMeasurementDetectsRetention() {
        class RetainedObject(@Suppress("unused") val a: Int, @Suppress("unused") val b: Int)

        val retained = ArrayList<RetainedObject>()

        val before = heapUsedAfterGc()
        repeat(iterations) { retained.add(RetainedObject(it, it)) }
        val after = heapUsedAfterGc()

        assertEquals(iterations, retained.size)
        val growth = after - before
        // we're allocating a million objects, each of which has at least 8 Bytes in size, so we should leak at least 8 * iterations bytes
        assertTrue(growth >= iterations * 8) {
            "Heap measurement is not sensitive enough: only ${growth / 1024 / 1024} MiB of growth measured for $iterations retained objects"
        }
    }
}
