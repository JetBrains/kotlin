// OUTPUT_DATA_FILE: cleaner_leak_without_checker.out
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*

import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner

// This cleaner won't be run, because it's deinitialized with globals after
// cleaners are disabled.
val globalCleaner = createCleaner(42) {
    println(it)
}

fun main() {
    // Make sure cleaner is initialized.
    assertNotNull(globalCleaner)
}
