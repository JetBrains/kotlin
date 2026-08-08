// KIND: STANDALONE
// WITH_PLATFORM_LIBS
// MODULE: ExistentialWrapperViaObjCInterop
// FILE: main.kt

import platform.darwin.NSObject

// todo: remove me after KT-87457
interface Anchor {
    fun ping(): Int
}

private class Hidden

fun crossNonExportedClassIntoObjC(): Boolean = NSObject().isEqual(Hidden())
