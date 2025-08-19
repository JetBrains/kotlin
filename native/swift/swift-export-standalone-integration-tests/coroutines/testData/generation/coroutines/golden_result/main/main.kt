@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___demo")
public fun __root___demo(): kotlin.native.internal.NativePtr {
    val _result = demo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
