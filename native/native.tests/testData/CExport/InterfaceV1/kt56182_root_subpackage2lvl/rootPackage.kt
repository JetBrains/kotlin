// This test is similar to kt42397, just everything is doubled: in "package knlibrary.subpackage" and the root package
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

object A {}

class B {
    companion object {}
}

fun enableMemoryChecker() {
    Platform.isMemoryLeakCheckerActive = true
}
