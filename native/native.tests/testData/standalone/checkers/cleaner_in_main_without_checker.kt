// OUTPUT_DATA_FILE: cleaner_in_main_without_checker.out
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.ref.createCleaner
import kotlin.native.Platform

fun main() {
    // Cleaner holds onto a finalization lambda. If it doesn't get executed,
    // the memory will leak. Suppress memory leak checker to check for cleaners
    // leak only.
    Platform.isMemoryLeakCheckerActive = false
    Platform.isCleanersLeakCheckerActive = false
    // This cleaner will not run, because with the checker inactive this cleaner
    // will not get collected before cleaners are disabled.
    createCleaner(42) {
        println(it)
    }
}
