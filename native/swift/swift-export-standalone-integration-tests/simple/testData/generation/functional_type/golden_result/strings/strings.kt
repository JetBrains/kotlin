@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("strings_internal_functional_type_callee_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
internal external fun strings_internal_functional_type_callee_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ExportedBridge("__root___consume_block_with_string_id__TypesOfArguments__U28Swift_StringU29202D_U20Swift_String__")
public fun __root___consume_block_with_string_id__TypesOfArguments__U28Swift_StringU29202D_U20Swift_String__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = strings_internal_functional_type_callee_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(closureBox.objcPtr(), _arg0)
            interpretObjCPointer<kotlin.String>(_result)
        }
    }
    val _result = run { consume_block_with_string_id(__block) }
    return _result.objcPtr()
}
