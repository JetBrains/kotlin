@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__")
public fun __root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Boolean>(i);
        { arg0: Int ->
            val _result = kotlinFun(arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { foo(__i) }
    return run { _result; true }
}

@ExportedBridge("__root___fooAny__TypesOfArguments__U28anyU20KotlinRuntimeSupport__KotlinBridgeableU29202D_U20Swift_Void__")
public fun __root___fooAny__TypesOfArguments__U28anyU20KotlinRuntimeSupport__KotlinBridgeableU29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(i);
        { arg0: kotlin.Any ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            run<Unit> { _result }
        }
    }
    val _result = run { fooAny(__i) }
    return run { _result; true }
}

@ExportedBridge("__root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__")
public fun __root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(i);
        { arg0: kotlin.collections.List<Int> ->
            val _result = kotlinFun(arg0.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { fooList(__i) }
    return run { _result; true }
}

@ExportedBridge("__root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__")
public fun __root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(i);
        { arg0: kotlin.String? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else arg0.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { fooString(__i) }
    return run { _result; true }
}
