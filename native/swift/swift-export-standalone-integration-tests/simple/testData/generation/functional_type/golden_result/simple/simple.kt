@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___closure_property_get")
public fun __root___closure_property_get(): kotlin.native.internal.NativePtr {
    val _result = closure_property
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(newValue);
        {
            val _result = kotlinFun()
            Unit
        }
    }
    closure_property = __newValue
}

@ExportedBridge("__root___foo_1")
public fun __root___foo_1(): kotlin.native.internal.NativePtr {
    val _result = foo_1()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo_consume_producing__TypesOfArguments__U2829202D_U202829202D_U20Swift_Void__")
public fun __root___foo_consume_producing__TypesOfArguments__U2829202D_U202829202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Unit {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->kotlin.native.internal.NativePtr>(block);
        {
            val _result = kotlinFun()
            run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(_result);
        {
            val _result = kotlinFun()
            Unit
        }
    }
        }
    }
    foo_consume_producing(__block)
}

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

@ExportedBridge("simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Unit {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    (__pointerToBlock as Function0<Unit>).invoke()
}
