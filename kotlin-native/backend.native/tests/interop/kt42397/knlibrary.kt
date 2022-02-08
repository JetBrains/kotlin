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
