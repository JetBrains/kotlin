@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__")
internal external fun typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Int

@ImportedBridge("typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): Int

@ImportedBridge("typealias_to_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__")
internal external fun typealias_to_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(pointerToClosure: kotlin.native.internal.NativePtr, _1: Int, _2: Int): Boolean

@ExportedBridge("__root___foo_flow_with_callback__TypesOfArguments__U2840escapingU202829202D_U20Swift_Int32U29202D_U20Swift_Int32__")
public fun __root___foo_flow_with_callback__TypesOfArguments__U2840escapingU202829202D_U20Swift_Int32U29202D_U20Swift_Int32__(callback: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __callback = run {
        val closurePtr = callback;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Function0<Int> ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__(closureBox.objcPtr(), _arg0)
            _result
        }
    }
    val _result = run { foo_flow_with_callback(__callback) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___typealias_demo__TypesOfArguments__U28Swift_Int32_U20Swift_Int32U29202D_U20Swift_Void__")
public fun __root___typealias_demo__TypesOfArguments__U28Swift_Int32_U20Swift_Int32U29202D_U20Swift_Void__(input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __input = run {
        val closurePtr = input;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Int, arg1: Int ->
            val _arg0 = arg0
            val _arg1 = arg1
            val _result = typealias_to_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(closureBox.objcPtr(), _arg0, _arg1)
            run<Unit> { _result }
        }
    }
    val _result = run { typealias_demo(__input) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealias_to_closure_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun typealias_to_closure_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Int {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = run { (__pointerToBlock as Function0<Int>).invoke() }
    return _result
}

@ExportedBridge("typealias_to_closure_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__")
public fun typealias_to_closure_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Int {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = run {
        val closurePtr = _1;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            _result
        }
    }
    val _result = run { (__pointerToBlock as Function1<Function0<Int>, Int>).invoke(___1) }
    return _result
}

@ExportedBridge("typealias_to_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__")
public fun typealias_to_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Int, _2: Int): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val ___2 = _2
    val _result = run { (__pointerToBlock as Function2<Int, Int, Unit>).invoke(___1, ___2) }
    return run { _result; true }
}
