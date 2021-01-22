import leakMemory.*
import kotlin.native.concurrent.*
import kotlin.native.Platform
import kotlin.test.*
import kotlinx.cinterop.*

val global = AtomicInt(0)

fun ensureInititalized() {
    kotlin.native.initRuntimeIfNeeded()
    // Leak memory
    StableRef.create(Any())
    global.value = 1
}

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    kotlin.native.internal.Debugging.forceCheckedShutdown = true
    assertTrue(global.value == 0)
    // Created a thread, made sure Kotlin is initialized there.
    test_RunInNewThread(staticCFunction(::ensureInititalized))
    assertTrue(global.value == 1)
    // Now exiting. With checked shutdown we will fail, complaining there're
    // unfinished threads with runtimes.
}
