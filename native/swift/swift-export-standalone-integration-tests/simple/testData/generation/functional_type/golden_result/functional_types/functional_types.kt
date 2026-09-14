@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("functional_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Void__")
internal external fun functional_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Void__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ExportedBridge("__root___consume_block_consuming_block__TypesOfArguments__U2840escapingU202829202D_U20Swift_VoidU29202D_U20Swift_Void__")
public fun __root___consume_block_consuming_block__TypesOfArguments__U2840escapingU202829202D_U20Swift_VoidU29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Function0<Unit> ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = functional_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Void__(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { consume_block_consuming_block(__block) }
    return run { _result; true }
}

@ExportedBridge("functional_types_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun functional_types_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = run { (__pointerToBlock as Function0<Unit>).invoke() }
    return run { _result; true }
}
