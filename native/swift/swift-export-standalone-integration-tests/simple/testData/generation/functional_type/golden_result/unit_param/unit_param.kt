@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__")
internal external fun unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr, _2: Boolean): Boolean

@ImportedBridge("unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__")
internal external fun unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToClosure: kotlin.native.internal.NativePtr, _1: Boolean): Boolean

@ExportedBridge("__root___bar")
public fun __root___bar(): kotlin.native.internal.NativePtr {
    val _result = run { bar() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___barIn__TypesOfArguments__U28Swift_String_U20Swift_VoidU29202D_U20Swift_Void__")
public fun __root___barIn__TypesOfArguments__U28Swift_String_U20Swift_VoidU29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.String, arg1: Unit ->
            val _arg0 = arg0.objcPtr()
            val _arg1 = run { arg1; true }
            val _result = unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__(closureBox.objcPtr(), _arg0, _arg1)
            run<Unit> { _result }
        }
    }
    val _result = run { barIn(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___baz")
public fun __root___baz(): kotlin.native.internal.NativePtr {
    val _result = run { baz() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo")
public fun __root___foo(): kotlin.native.internal.NativePtr {
    val _result = run { foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooIn__TypesOfArguments__U28Swift_VoidU29202D_U20Swift_Void__")
public fun __root___fooIn__TypesOfArguments__U28Swift_VoidU29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Unit ->
            val _arg0 = run { arg0; true }
            val _result = unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { fooIn(__block) }
    return run { _result; true }
}

@ExportedBridge("unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__")
public fun unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Boolean): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = run<Unit> { _1 }
    val _result = run { (__pointerToBlock as Function1<Unit, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__")
public fun unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr, _2: Boolean): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = interpretObjCPointer<kotlin.String>(_1)
    val ___2 = run<Unit> { _2 }
    val _result = run { (__pointerToBlock as Function2<kotlin.String, Unit, Unit>).invoke(___1, ___2) }
    return run { _result; true }
}

@ExportedBridge("unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_String_U20Swift_VoidU29202D_U20Swift_Void__")
public fun unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_String_U20Swift_VoidU29202D_U20Swift_Void__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = run {
        val closurePtr = _1;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.String, arg1: Unit ->
            val _arg0 = arg0.objcPtr()
            val _arg1 = run { arg1; true }
            val _result = unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__(closureBox.objcPtr(), _arg0, _arg1)
            run<Unit> { _result }
        }
    }
    val _result = run { (__pointerToBlock as Function1<Function2<kotlin.String, Unit, Unit>, Unit>).invoke(___1) }
    return run { _result; true }
}
