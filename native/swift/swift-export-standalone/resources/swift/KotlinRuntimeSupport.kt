@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root____getExceptionMessage__TypesOfArguments__ExportedKotlinPackages_kotlin_Exception__")
public fun __root____getExceptionMessage__TypesOfArguments__ExportedKotlinPackages_kotlin_Exception__(exception: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __exception = kotlin.native.internal.ref.dereferenceExternalRCRef(exception) as kotlin.Exception
    val _result = __exception.message
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}