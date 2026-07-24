@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___today")
public fun __root___today(): kotlin.native.internal.NativePtr {
    val _result = run { today() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
