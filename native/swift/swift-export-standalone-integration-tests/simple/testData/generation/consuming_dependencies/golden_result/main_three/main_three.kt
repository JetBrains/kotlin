@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___deps_instance_3_get")
public fun __root___deps_instance_3_get(): kotlin.native.internal.NativePtr {
    val _result = deps_instance_3
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

