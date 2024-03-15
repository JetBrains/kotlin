// DISABLE_NATIVE: gcType=NOOP
// EXIT_CODE: !0
// OUTPUT_REGEX: Cleaner (0x)?[0-9a-fA-F]+ was disposed during program exit.*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner
import kotlin.native.Platform

@ThreadLocal
var tlsCleaner: Cleaner? = null

fun main() {
    // Cleaner holds onto a finalization lambda. If it doesn't get executed,
    // the memory will leak. Suppress memory leak checker to check for cleaners
    // leak only.
    Platform.isMemoryLeakCheckerActive = false
    Platform.isCleanersLeakCheckerActive = true
    // This cleaner won't be run
    tlsCleaner = createCleaner(42) {
        println(it)
    }
}
