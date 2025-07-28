@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___consume_block_with_byte_id__TypesOfArguments__U28Swift_Int8U29202D_U20Swift_Int8__")
public fun __root___consume_block_with_byte_id__TypesOfArguments__U28Swift_Int8U29202D_U20Swift_Int8__(block: kotlin.native.internal.NativePtr): Byte {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Byte)->Byte>(block);
        { arg0: Byte ->
            val _result = kotlinFun(arg0)
            _result
        }
    }
    val _result = consume_block_with_byte_id(__block)
    return _result
}

@ExportedBridge("__root___consume_block_with_uint_id__TypesOfArguments__U28Swift_UInt32U29202D_U20Swift_UInt32__")
public fun __root___consume_block_with_uint_id__TypesOfArguments__U28Swift_UInt32U29202D_U20Swift_UInt32__(block: kotlin.native.internal.NativePtr): UInt {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(UInt)->UInt>(block);
        { arg0: UInt ->
            val _result = kotlinFun(arg0)
            _result
        }
    }
    val _result = consume_block_with_uint_id(__block)
    return _result
}
