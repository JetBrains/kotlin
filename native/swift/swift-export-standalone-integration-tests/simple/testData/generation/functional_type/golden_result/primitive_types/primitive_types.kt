@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___consume_block_with_Unit_id__TypesOfArguments__U28Swift_VoidU29202D_U20Swift_Int8__")
public fun __root___consume_block_with_Unit_id__TypesOfArguments__U28Swift_VoidU29202D_U20Swift_Int8__(block: kotlin.native.internal.NativePtr): Byte {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Unit)->Byte>(block);
        { arg0: Unit ->
            val _result = kotlinFun(arg0)
            _result
        }
    }
    val _result = consume_block_with_Unit_id(__block)
    return _result
}

@ExportedBridge("__root___consume_block_with_Unit_idInTheMix1__TypesOfArguments__U28Swift_Void_U20Swift_Int8U29202D_U20Swift_Int8__")
public fun __root___consume_block_with_Unit_idInTheMix1__TypesOfArguments__U28Swift_Void_U20Swift_Int8U29202D_U20Swift_Int8__(block: kotlin.native.internal.NativePtr): Byte {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Unit, Byte)->Byte>(block);
        { arg0: Unit, arg1: Byte ->
            val _result = kotlinFun(arg0, arg1)
            _result
        }
    }
    val _result = consume_block_with_Unit_idInTheMix1(__block)
    return _result
}

@ExportedBridge("__root___consume_block_with_Unit_idInTheMix2__TypesOfArguments__U28Swift_Int8_U20Swift_VoidU29202D_U20Swift_Int8__")
public fun __root___consume_block_with_Unit_idInTheMix2__TypesOfArguments__U28Swift_Int8_U20Swift_VoidU29202D_U20Swift_Int8__(block: kotlin.native.internal.NativePtr): Byte {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Byte, Unit)->Byte>(block);
        { arg0: Byte, arg1: Unit ->
            val _result = kotlinFun(arg0, arg1)
            _result
        }
    }
    val _result = consume_block_with_Unit_idInTheMix2(__block)
    return _result
}

@ExportedBridge("__root___consume_block_with_Unit_idInTheMix3__TypesOfArguments__U28Swift_Void_U20Swift_VoidU29202D_U20Swift_Int8__")
public fun __root___consume_block_with_Unit_idInTheMix3__TypesOfArguments__U28Swift_Void_U20Swift_VoidU29202D_U20Swift_Int8__(block: kotlin.native.internal.NativePtr): Byte {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Unit, Unit)->Byte>(block);
        { arg0: Unit, arg1: Unit ->
            val _result = kotlinFun(arg0, arg1)
            _result
        }
    }
    val _result = consume_block_with_Unit_idInTheMix3(__block)
    return _result
}

@ExportedBridge("__root___consume_block_with_Unit_idInTheMix4__TypesOfArguments__U28Swift_Int8_U20Swift_Void_U20Swift_Int8U29202D_U20Swift_Int8__")
public fun __root___consume_block_with_Unit_idInTheMix4__TypesOfArguments__U28Swift_Int8_U20Swift_Void_U20Swift_Int8U29202D_U20Swift_Int8__(block: kotlin.native.internal.NativePtr): Byte {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Byte, Unit, Byte)->Byte>(block);
        { arg0: Byte, arg1: Unit, arg2: Byte ->
            val _result = kotlinFun(arg0, arg1, arg2)
            _result
        }
    }
    val _result = consume_block_with_Unit_idInTheMix4(__block)
    return _result
}

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
