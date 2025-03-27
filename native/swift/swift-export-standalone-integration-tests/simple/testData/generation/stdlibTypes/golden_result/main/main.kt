@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("generation_stdlibTypes_stdlibTypes_buildString__TypesOfArguments__ExportedKotlinPackages_kotlin_text_StringBuilder__")
public fun generation_stdlibTypes_stdlibTypes_buildString__TypesOfArguments__ExportedKotlinPackages_kotlin_text_StringBuilder__(sb: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __sb = kotlin.native.internal.ref.dereferenceExternalRCRef(sb) as kotlin.text.StringBuilder
    val _result = generation.stdlibTypes.stdlibTypes.buildString(__sb)
    return _result.objcPtr()
}

@ExportedBridge("generation_stdlibTypes_stdlibTypes_returnsByteArray")
public fun generation_stdlibTypes_stdlibTypes_returnsByteArray(): kotlin.native.internal.NativePtr {
    val _result = generation.stdlibTypes.stdlibTypes.returnsByteArray()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
