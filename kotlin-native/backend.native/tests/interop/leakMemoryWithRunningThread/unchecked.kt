import leakMemory.*
import kotlin.native.concurrent.*
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
    kotlin.native.internal.Debugging.forceCheckedShutdown = false
    assertTrue(global.value == 0)
    // Created a thread, made sure Kotlin is initialized there.
    test_RunInNewThread(staticCFunction(::ensureInititalized))
    assertTrue(global.value == 1)
    // Now exiting. With unchecked shutdown, we exit quietly, even though there're
    // unfinished threads with runtimes.
}
