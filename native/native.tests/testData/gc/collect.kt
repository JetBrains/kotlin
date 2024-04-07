import kotlin.native.runtime.GC
import kotlin.test.*

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@Test
fun test() {
    GC.collect()
}