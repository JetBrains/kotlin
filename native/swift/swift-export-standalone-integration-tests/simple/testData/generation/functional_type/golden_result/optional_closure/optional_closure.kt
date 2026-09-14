@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyInterface::class, "_optional_closure_MyInterface")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void_____reverse_swift")
internal external fun MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void_____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(MyInterface::class, "foo")
public fun MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void_____reverse(self: MyInterface, arg: Function0<Unit>?): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = if (arg == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg)
    val _result = MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void_____reverse_swift(__self, __arg)
    return run<Unit> { _result }
}

@ImportedBridge("optional_closure_internal_functional_type_callee_SwiftU2EOptionalU3C2829202D3E20SwiftU2EVoidU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun optional_closure_internal_functional_type_callee_SwiftU2EOptionalU3C2829202D3E20SwiftU2EVoidU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_U2829202D_U20Swift_String___")
internal external fun optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_U2829202D_U20Swift_String___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): Boolean

@ExportedBridge("MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___")
public fun MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyInterface
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else run {
        val closurePtr = arg;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { __self.foo(__arg) }
    return run { _result; true }
}

@ExportedBridge("__root___consume_consuming_opt_closure__TypesOfArguments__Swift_Optional_U28Swift_Optional_U2829202D_U20Swift_String_U29202D_U20Swift_Void___")
public fun __root___consume_consuming_opt_closure__TypesOfArguments__Swift_Optional_U28Swift_Optional_U2829202D_U20Swift_String_U29202D_U20Swift_Void___(arg: kotlin.native.internal.NativePtr): Boolean {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else run {
        val closurePtr = arg;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Function0<kotlin.String>? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_U2829202D_U20Swift_String___(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { consume_consuming_opt_closure(__arg) }
    return run { _result; true }
}

@ExportedBridge("__root___consume_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___")
public fun __root___consume_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(arg: kotlin.native.internal.NativePtr): Boolean {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else run {
        val closurePtr = arg;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { consume_opt_closure(__arg) }
    return run { _result; true }
}

@ExportedBridge("__root___consume_producing_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Optional_U2829202D_U20Swift_Void____")
public fun __root___consume_producing_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Optional_U2829202D_U20Swift_Void____(arg: kotlin.native.internal.NativePtr): Boolean {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else run {
        val closurePtr = arg;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = optional_closure_internal_functional_type_callee_SwiftU2EOptionalU3C2829202D3E20SwiftU2EVoidU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            if (_result == kotlin.native.internal.NativePtr.NULL) null else run {
                val closurePtr = _result;
                val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
                {
                    val _result = optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
                    run<Unit> { _result }
                }
            }
        }
    }
    val _result = run { consume_producing_opt_closure(__arg) }
    return run { _result; true }
}

@ExportedBridge("__root___produce_opt_closure__TypesOfArguments__Swift_Void__")
public fun __root___produce_opt_closure__TypesOfArguments__Swift_Void__(arg: Boolean): kotlin.native.internal.NativePtr {
    val __arg = run<Unit> { arg }
    val _result = run { produce_opt_closure(__arg) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = run { (__pointerToBlock as Function0<kotlin.String>).invoke() }
    return _result.objcPtr()
}

@ExportedBridge("optional_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun optional_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = run { (__pointerToBlock as Function0<Unit>).invoke() }
    return run { _result; true }
}
