@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("generation_stdlibTypes_stdlibTypes_returnsByteArray")
public fun generation_stdlibTypes_stdlibTypes_returnsByteArray(): kotlin.native.internal.NativePtr {
    val _result = generation.stdlibTypes.stdlibTypes.returnsByteArray()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
