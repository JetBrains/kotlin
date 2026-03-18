@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___bar")
public fun __root___bar(): kotlin.native.internal.NativePtr {
    val _result = run { bar() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo")
public fun __root___foo(): kotlin.native.internal.NativePtr {
    val _result = run { foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
