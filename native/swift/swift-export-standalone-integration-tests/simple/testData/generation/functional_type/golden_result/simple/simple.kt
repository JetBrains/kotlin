@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32___")
internal external fun simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("simple_internal_functional_type_callee_U2829202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun simple_internal_functional_type_callee_U2829202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ExportedBridge("__root___closure_property_get")
public fun __root___closure_property_get(): kotlin.native.internal.NativePtr {
    val _result = run { closure_property }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = run {
        val closurePtr = newValue;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { closure_property = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___foo_1")
public fun __root___foo_1(): kotlin.native.internal.NativePtr {
    val _result = run { foo_1() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo_consume_consuming__TypesOfArguments__U2840escapingU2028Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__")
public fun __root___foo_consume_consuming__TypesOfArguments__U2840escapingU2028Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Function2<UInt, UInt, kotlin.ranges.IntRange> ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32___(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { foo_consume_consuming(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___foo_consume_consuming_2__TypesOfArguments__U2840escapingU2028Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__")
public fun __root___foo_consume_consuming_2__TypesOfArguments__U2840escapingU2028Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Function2<UInt, UInt, kotlin.ranges.IntRange> ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32___(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { foo_consume_consuming_2(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___foo_consume_producing__TypesOfArguments__U2829202D_U202829202D_U20Swift_Void__")
public fun __root___foo_consume_producing__TypesOfArguments__U2829202D_U202829202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = simple_internal_functional_type_callee_U2829202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            run {
                val closurePtr = _result;
                val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
                {
                    val _result = simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
                    run<Unit> { _result }
                }
            }
        }
    }
    val _result = run { foo_consume_producing(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { foo_consume_simple(__block) }
    return run { _result; true }
}

@ExportedBridge("kotlin_ranges_intRange_create_int_simple")
fun kotlin_ranges_intRange_create_int_simple(start: Int, end: Int): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(start .. end)
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
    val _result = run { (__pointerToBlock as Function2<UInt, UInt, kotlin.ranges.IntRange>).invoke(___1, ___2) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = run { (__pointerToBlock as Function0<Unit>).invoke() }
    return run { _result; true }
}
