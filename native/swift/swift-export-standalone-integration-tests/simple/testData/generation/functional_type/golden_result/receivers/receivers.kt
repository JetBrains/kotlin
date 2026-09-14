@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___")
internal external fun receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
internal external fun receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToClosure: kotlin.native.internal.NativePtr, _1: Int): Boolean

@ImportedBridge("receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
internal external fun receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__")
internal external fun receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ExportedBridge("__root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__")
public fun __root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = run {
        val closurePtr = i;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Int ->
            val _arg0 = arg0
            val _result = receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { foo(__i) }
    return run { _result; true }
}

@ExportedBridge("__root___fooAny__TypesOfArguments__U28anyU20KotlinRuntimeSupport__KotlinBridgeableU29202D_U20Swift_Void__")
public fun __root___fooAny__TypesOfArguments__U28anyU20KotlinRuntimeSupport__KotlinBridgeableU29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = run {
        val closurePtr = i;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.Any ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { fooAny(__i) }
    return run { _result; true }
}

@ExportedBridge("__root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__")
public fun __root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = run {
        val closurePtr = i;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.collections.List<Int> ->
            val _arg0 = arg0.objcPtr()
            val _result = receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { fooList(__i) }
    return run { _result; true }
}

@ExportedBridge("__root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__")
public fun __root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = run {
        val closurePtr = i;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.String? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else arg0.objcPtr()
            val _result = receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { fooString(__i) }
    return run { _result; true }
}
