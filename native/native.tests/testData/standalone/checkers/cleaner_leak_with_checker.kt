// DISABLE_NATIVE: gcType=NOOP
// EXIT_CODE: !0
// OUTPUT_REGEX: Cleaner (0x)?[0-9a-fA-F]+ was disposed during program exit.*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*

import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner
import kotlin.native.Platform

// This cleaner won't be run, because it's deinitialized with globals after
// cleaners are disabled.
val globalCleaner = createCleaner(42) {
    println(it)
}

fun main() {
    Platform.isCleanersLeakCheckerActive = true
    // Make sure cleaner is initialized.
    assertNotNull(globalCleaner)
}
