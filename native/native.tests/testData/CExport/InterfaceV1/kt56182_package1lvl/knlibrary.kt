// This test is similar to kt42397, just everything is doubled: in "package knlibrary" and the root package

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package knlibrary

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
