// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import platform.Foundation.*

private fun readString(path: String): String? {
    return memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, error.ptr)
    }
}

fun box(): String {
    // We don't want actually read anything, just testing for compilability
    readString("")
    return "OK"
}