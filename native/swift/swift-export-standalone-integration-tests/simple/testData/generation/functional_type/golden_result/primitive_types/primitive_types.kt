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

@ExportedBridge("__root___produce_block_with_byte_byte")
public fun __root___produce_block_with_byte_byte(): kotlin.native.internal.NativePtr {
    val _result = produce_block_with_byte_byte()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("primitive_types_internal_functional_type_caller_SwiftU2EInt8__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int8_Swift_Int8_Swift_Int8__")
public fun primitive_types_internal_functional_type_caller_SwiftU2EInt8__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int8_Swift_Int8_Swift_Int8__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Byte, _2: Byte, _3: Byte): Byte {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val ___2 = _2
    val ___3 = _3
    val _result = (__pointerToBlock as Function3<Byte, Byte, Byte, Byte>).invoke(___1, ___2, ___3)
    return _result
}
