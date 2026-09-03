@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___badHiddenList")
public fun __root___badHiddenList(): kotlin.native.internal.NativePtr {
    val _result = run { badHiddenList() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___badKClassList")
public fun __root___badKClassList(): kotlin.native.internal.NativePtr {
    val _result = run { badKClassList() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
