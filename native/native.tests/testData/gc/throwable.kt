// DISABLE_NATIVE: gcType=NOOP
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC
import kotlin.native.NoInline
import kotlin.test.*

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@NoInline
private fun makeWeakRef(create: () -> Any) = WeakReference(create())

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)
private fun ensureGetsCollected(create: () -> Any) {
    val ref = makeWeakRef(create)
    GC.collect()
    assertNull(ref.get())
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@Test
fun test() {
    ensureGetsCollected { Throwable() }
    ensureGetsCollected {
        val throwable = Throwable()
        throwable.getStackTrace()
        throwable
    }
}