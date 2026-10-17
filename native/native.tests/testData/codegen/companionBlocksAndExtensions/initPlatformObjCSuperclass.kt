// TARGET_BACKEND: NATIVE
// LANGUAGE: +CompanionBlocks
// Only interested, that this compiles. See initObjC* for initialization order with custom classes.
import platform.darwin.NSObject
import platform.Foundation.NSURLSessionDataDelegateProtocol

class C : NSObject(), NSURLSessionDataDelegateProtocol {
    fun foo() = "OK"
}

fun box() = C().foo()
