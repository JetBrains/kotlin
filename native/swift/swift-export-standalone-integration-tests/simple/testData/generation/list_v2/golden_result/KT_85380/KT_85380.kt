@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___badHiddenList")
public fun __root___badHiddenList(): kotlin.native.internal.NativePtr {
    val _result = badHiddenList()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___badKClassList")
public fun __root___badKClassList(): kotlin.native.internal.NativePtr {
    val _result = badKClassList()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
