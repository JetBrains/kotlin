@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
import kotlin.native.runtime.GC
import cinterop.*

// inspired by KT-73947

fun produce(): List<Any> {
    return List(100_000) { Any() }
}

fun consume(x: Any?) {
    useArray(x as List<Any?>);
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
fun main() {
    task(::produce, GC::collect, GC::schedule, ::consume);

}
