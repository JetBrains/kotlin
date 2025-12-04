@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___consume_consuming_opt_closure__TypesOfArguments__Swift_Optional_U28Swift_Optional_U2829202D_U20Swift_String_U29202D_U20Swift_Void___")
public fun __root___consume_consuming_opt_closure__TypesOfArguments__Swift_Optional_U28Swift_Optional_U2829202D_U20Swift_String_U29202D_U20Swift_Void___(arg: kotlin.native.internal.NativePtr): Unit {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(arg);
        { arg0: Function0<kotlin.String>? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            Unit
        }
    }
    consume_consuming_opt_closure(__arg)
}

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

@ExportedBridge("__root___produce_opt_closure__TypesOfArguments__Swift_Void__")
public fun __root___produce_opt_closure__TypesOfArguments__Swift_Void__(): kotlin.native.internal.NativePtr {
    val __arg = Unit
    val _result = produce_opt_closure(__arg)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = (__pointerToBlock as Function0<kotlin.String>).invoke()
    return _result.objcPtr()
}
