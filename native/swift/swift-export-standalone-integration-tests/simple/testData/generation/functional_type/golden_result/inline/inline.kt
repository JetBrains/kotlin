@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("inline_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun inline_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): Boolean

@ExportedBridge("__root___bar__TypesOfArguments__U2829202D_U20Swift_Void_U2829202D_U20Swift_Void__")
public fun __root___bar__TypesOfArguments__U2829202D_U20Swift_Void_U2829202D_U20Swift_Void__(inlined: kotlin.native.internal.NativePtr, notInlined: kotlin.native.internal.NativePtr): Boolean {
    val __inlined = run {
        val closurePtr = inlined;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = inline_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            run<Unit> { _result }
        }
    }
    val __notInlined = run {
        val closurePtr = notInlined;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = inline_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { bar(__inlined, __notInlined) }
    return run { _result; true }
}

@ExportedBridge("__root___foo__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___foo__TypesOfArguments__U2829202D_U20Swift_Void__(inlined: kotlin.native.internal.NativePtr): Boolean {
    val __inlined = run {
        val closurePtr = inlined;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = inline_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { foo(__inlined) }
    return run { _result; true }
}
