@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.*

actual class MyExpectClass {
    actual val myExpectClassProperty: Int = 0
    val myNativeProperty: CPointer<CEnumVar> = TODO()
}