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

@ExportedBridge("__root___foo_consume_consuming__TypesOfArguments__U2828Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__")
public fun __root___foo_consume_consuming__TypesOfArguments__U2828Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Unit {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(block);
        { arg0: Function2<UInt, UInt, kotlin.ranges.IntRange> ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            Unit
        }
    }
    foo_consume_consuming(__block)
}

@ExportedBridge("__root___foo_consume_consuming_2__TypesOfArguments__U2828Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__")
public fun __root___foo_consume_consuming_2__TypesOfArguments__U2828Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Unit {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(block);
        { arg0: Function2<UInt, UInt, kotlin.ranges.IntRange> ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            Unit
        }
    }
    foo_consume_consuming_2(__block)
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

@ExportedBridge("kotlin_ranges_intRange_getEndInclusive_int_simple")
fun kotlin_ranges_intRange_getEndInclusive_int_simple(nativePtr: kotlin.native.internal.NativePtr): Int {
    val intRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as IntRange
    return intRange.endInclusive
}

@ExportedBridge("kotlin_ranges_intRange_getStart_int_simple")
fun kotlin_ranges_intRange_getStart_int_simple(nativePtr: kotlin.native.internal.NativePtr): Int {
    val intRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as IntRange
    return intRange.start
}

@ExportedBridge("simple_internal_functional_type_caller_SwiftU2EClosedRangeU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32_Swift_UInt32__")
public fun simple_internal_functional_type_caller_SwiftU2EClosedRangeU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32_Swift_UInt32__(pointerToBlock: kotlin.native.internal.NativePtr, _1: UInt, _2: UInt): kotlin.native.internal.NativePtr {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val ___2 = _2
    val _result = (__pointerToBlock as Function2<UInt, UInt, kotlin.ranges.IntRange>).invoke(___1, ___2)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Unit {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    (__pointerToBlock as Function0<Unit>).invoke()
}
