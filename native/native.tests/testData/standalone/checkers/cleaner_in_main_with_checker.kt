// OUTPUT_DATA_FILE: cleaner_in_main_with_checker.out
// DISABLE_NATIVE: gcType=NOOP
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner
import kotlin.native.Platform

fun main() {
    // Cleaner holds onto a finalization lambda. If it doesn't get executed,
    // the memory will leak. Suppress memory leak checker to check for cleaners
    // leak only.
    Platform.isMemoryLeakCheckerActive = false
    Platform.isCleanersLeakCheckerActive = true
    // This cleaner will run, because with the checker active this cleaner
    // will get collected, block scheduled and executed before cleaners are disabled.
    createCleaner(42) {
        println(it)
    }
}
