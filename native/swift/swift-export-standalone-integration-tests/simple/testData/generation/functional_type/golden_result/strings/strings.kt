@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___consume_block_with_string_id__TypesOfArguments__U28Swift_StringU29202D_U20Swift_String__")
public fun __root___consume_block_with_string_id__TypesOfArguments__U28Swift_StringU29202D_U20Swift_String__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(block);
        { arg0: kotlin.String -> val _result = kotlinFun(arg0.objcPtr()); interpretObjCPointer<kotlin.String>(_result) }
    }
    val _result = consume_block_with_string_id(__block)
    return _result.objcPtr()
}
