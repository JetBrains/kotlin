// This test is similar to kt42397, just everything is in "package knlibrary.subpackage" instead of "package knlibrary"
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package knlibrary.subpackage

import kotlin.native.Platform

// The following 2 singletons are unused. However, since we are generating C bindings for them,
// they should be marked as used, so that the code generator emits their deinitialization.

object A {}

class B {
    companion object {}
}

fun enableMemoryChecker() {
    Platform.isMemoryLeakCheckerActive = true
}
