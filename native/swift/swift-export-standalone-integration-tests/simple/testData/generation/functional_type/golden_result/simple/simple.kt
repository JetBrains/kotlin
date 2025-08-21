@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Unit {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(block);
        {
            val _result = kotlinFun()
            Unit
        }
    }
    foo_consume_simple(__block)
}
