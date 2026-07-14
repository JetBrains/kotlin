// LANGUAGE: +CompanionBlocksAndExtensions
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// ^ This test is expected to fail with compilation error - AFU does not support properties w/o backing fields.

import kotlinx.atomicfu.*
import kotlin.test.*

public class Outer {
    companion {
        private val a: AtomicInt
            get() = atomic(0)
    }
}

fun box(): String = "OK"
