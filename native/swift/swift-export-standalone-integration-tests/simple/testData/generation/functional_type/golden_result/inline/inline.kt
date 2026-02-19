@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___bar__TypesOfArguments__U2829202D_U20Swift_Void_U2829202D_U20Swift_Void__")
public fun __root___bar__TypesOfArguments__U2829202D_U20Swift_Void_U2829202D_U20Swift_Void__(inlined: kotlin.native.internal.NativePtr, notInlined: kotlin.native.internal.NativePtr): Unit {
    val __inlined = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(inlined);
        {
            val _result = kotlinFun()
            Unit
        }
    }
    val __notInlined = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(notInlined);
        {
            val _result = kotlinFun()
            Unit
        }
    }
    bar(__inlined, __notInlined)
}

@ExportedBridge("__root___foo__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___foo__TypesOfArguments__U2829202D_U20Swift_Void__(inlined: kotlin.native.internal.NativePtr): Unit {
    val __inlined = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(inlined);
        {
            val _result = kotlinFun()
            Unit
        }
    }
    foo(__inlined)
}
