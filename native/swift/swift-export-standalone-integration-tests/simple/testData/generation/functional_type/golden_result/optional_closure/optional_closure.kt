@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___consume_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___")
public fun __root___consume_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(arg: kotlin.native.internal.NativePtr): Unit {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(arg);
        {
            val _result = kotlinFun()
            Unit
        }
    }
    consume_opt_closure(__arg)
}

@ExportedBridge("__root___consume_producing_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Optional_U2829202D_U20Swift_Void____")
public fun __root___consume_producing_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Optional_U2829202D_U20Swift_Void____(arg: kotlin.native.internal.NativePtr): Unit {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->kotlin.native.internal.NativePtr>(arg);
        {
            val _result = kotlinFun()
            if (_result == kotlin.native.internal.NativePtr.NULL) null else run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(_result);
        {
            val _result = kotlinFun()
            Unit
        }
    }
        }
    }
    consume_producing_opt_closure(__arg)
}
