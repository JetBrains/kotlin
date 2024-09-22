@file:OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.runtime.GC
import kotlin.native.internal.isPermanent
import kotlin.test.*

private var _counter = 0

object Permanent {
    var counter
        get() = _counter
        set(value) {
            _counter = value
        }
}

fun assertIsPermanent() {
    assertTrue(Permanent.isPermanent())
}

fun stableRefsCount(): Long {
    GC.collect()
    return GC.lastGCInfo!!.rootSet.stableReferences
}
