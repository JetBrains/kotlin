// TARGET_BACKEND: NATIVE
// WITH_PLATFORM_LIBS
// DISABLE_NATIVE: isAppleTarget=false
import platform.darwin.NSObject

fun box(): String {
    // It doesn't matter what we really do here.
    // The test checks that importing and linking platform.darwin doesn't trigger a crash in certain modes.
    // See KT-82674.
    val nsObject = NSObject()
    return if (nsObject.isKindOfClass(NSObject)) {
        "OK"
    } else {
        "FAIL"
    }
}
