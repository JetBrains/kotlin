// DISABLE_NATIVE: gcType=NOOP
import kotlin.test.*
import kotlin.native.ref.*
import kotlin.native.NoInline

data class Data(val s: String)

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@NoInline
fun localWeak(): WeakReference<Data>  {
    val x = Data("Hello")
    val weak = WeakReference(x)
    assertSame(x, weak.get())
    return weak
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@NoInline
fun multiWeak(): Array<WeakReference<Data>>  {
    val x = Data("Hello")
    val weaks = Array(100, { WeakReference(x) } )
    weaks.forEach {
        it -> if (it.get()?.s != "Hello") throw Error("bad reference")
    }
    return weaks
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)
@Test fun runTest() {
    val weak = localWeak()
    kotlin.native.runtime.GC.collect()
    assertNull(weak.get())

    val weaks = multiWeak()

    kotlin.native.runtime.GC.collect()

    weaks.forEach {
        it -> if (it.get()?.s != null) throw Error("not null")
    }
}