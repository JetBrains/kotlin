@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___buildString__TypesOfArguments__ExportedKotlinPackages_kotlin_text_StringBuilder__")
public fun __root___buildString__TypesOfArguments__ExportedKotlinPackages_kotlin_text_StringBuilder__(sb: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __sb = kotlin.native.internal.ref.dereferenceExternalRCRef(sb) as kotlin.text.StringBuilder
    val _result = buildString(__sb)
    return _result.objcPtr()
}

@ExportedBridge("__root___returnsByteArray")
public fun __root___returnsByteArray(): kotlin.native.internal.NativePtr {
    val _result = returnsByteArray()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
