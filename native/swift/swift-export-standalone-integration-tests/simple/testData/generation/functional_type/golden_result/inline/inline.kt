@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___bar__TypesOfArguments__U2829202D_U20Swift_Void_U2829202D_U20Swift_Void__")
public fun __root___bar__TypesOfArguments__U2829202D_U20Swift_Void_U2829202D_U20Swift_Void__(inlined: kotlin.native.internal.NativePtr, notInlined: kotlin.native.internal.NativePtr): Boolean {
    val __inlined = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(inlined);
        {
            val _result = kotlinFun()
            run<Unit> { _result }
        }
    }
    val __notInlined = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(notInlined);
        {
            val _result = kotlinFun()
            run<Unit> { _result }
        }
    }
    val _result = run { bar(__inlined, __notInlined) }
    return run { _result; true }
}

@ExportedBridge("__root___foo__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___foo__TypesOfArguments__U2829202D_U20Swift_Void__(inlined: kotlin.native.internal.NativePtr): Boolean {
    val __inlined = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(inlined);
        {
            val _result = kotlinFun()
            run<Unit> { _result }
        }
    }
    val _result = run { foo(__inlined) }
    return run { _result; true }
}
