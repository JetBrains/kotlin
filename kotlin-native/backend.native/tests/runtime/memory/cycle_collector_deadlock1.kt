import kotlin.native.concurrent.*
import kotlin.test.*

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
fun main() {
    kotlin.native.runtime.GC.cyclicCollectorEnabled = true

    repeat(10000) {
        // Create atomic cyclic garbage:
        val ref = AtomicReference<Any?>(null)
        ref.value = ref
    }

    // main thread will then run cycle collector termination, which involves running it and cleaning everything up.
    // 10000 references should hit [kGcThreshold] then.
}