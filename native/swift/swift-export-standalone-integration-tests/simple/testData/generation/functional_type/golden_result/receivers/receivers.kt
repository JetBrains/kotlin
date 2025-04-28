@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__")
public fun __root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Unit {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Unit>(i);
        { arg0: Int ->
            kotlinFun(arg0)
        }
    }
    foo(__i)
}

@ExportedBridge("__root___fooAny__TypesOfArguments__U28KotlinRuntime_KotlinBaseU29202D_U20Swift_Void__")
public fun __root___fooAny__TypesOfArguments__U28KotlinRuntime_KotlinBaseU29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Unit {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(i);
        { arg0: kotlin.Any ->
            kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
        }
    }
    fooAny(__i)
}

@ExportedBridge("__root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__")
public fun __root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Unit {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(i);
        { arg0: kotlin.collections.List<Int> ->
            kotlinFun(arg0.objcPtr())
        }
    }
    fooList(__i)
}

@ExportedBridge("__root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__")
public fun __root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Unit {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(i);
        { arg0: kotlin.String? ->
            kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else arg0.objcPtr())
        }
    }
    fooString(__i)
}
