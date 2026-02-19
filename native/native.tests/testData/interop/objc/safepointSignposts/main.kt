@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
import cinterop.*
import kotlin.time.Duration.Companion.milliseconds

class MyClass: NSObject()

fun main() {
    kotlin.native.runtime.GC.regularGCInterval = 1.milliseconds
    kotlin.native.runtime.GC.schedule()
    runTest(MyClass(), 10_000_000)
}