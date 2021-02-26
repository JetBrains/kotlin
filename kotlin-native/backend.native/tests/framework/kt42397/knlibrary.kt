import kotlin.native.Platform

// The following 2 singletons are unused. However, since we are generating ObjC bindings for them,
// they should be marked as used, so that the code generator emits their deinitialization.

object A {
    fun foo() = 1
}

class B {
    companion object {
        fun foo() = 2
    }
}

fun enableMemoryChecker() {
    Platform.isMemoryLeakCheckerActive = true
}
