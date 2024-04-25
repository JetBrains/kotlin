// DISABLE_NATIVE: gcType=NOOP

import kotlin.test.*
import kotlin.native.ref.*
import kotlin.native.NoInline

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)
@Test fun runTest() {
    val weakRefToTrashCycle = createLoop()
    kotlin.native.runtime.GC.collect()
    assertNull(weakRefToTrashCycle.get())
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@NoInline
private fun createLoop(): WeakReference<Any> {
    val loop = Array<Any?>(1, { null })
    loop[0] = loop

    return WeakReference(loop)
}