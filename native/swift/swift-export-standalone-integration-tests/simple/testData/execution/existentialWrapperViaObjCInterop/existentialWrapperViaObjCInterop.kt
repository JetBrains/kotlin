// KIND: STANDALONE
// WITH_PLATFORM_LIBS
// MODULE: ExistentialWrapperViaObjCInterop
// FILE: main.kt

import platform.darwin.NSObject

private class Hidden
fun crossNonExportedClassIntoObjC(): Boolean = NSObject().isEqual(Hidden())
