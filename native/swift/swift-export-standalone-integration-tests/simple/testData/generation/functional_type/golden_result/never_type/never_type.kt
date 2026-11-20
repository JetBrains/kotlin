@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("never_type_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_Swift_Never___")
internal external fun never_type_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_Swift_Never___(pointerToClosure: kotlin.native.internal.NativePtr, _1: Int, _2: Boolean): Int

@ExportedBridge("__root___consume_block_with_never__TypesOfArguments__U28Swift_NeverU29202D_U20Swift_Never__")
public fun __root___consume_block_with_never__TypesOfArguments__U28Swift_NeverU29202D_U20Swift_Never__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Nothing ->
            val _arg0 = arg0
            val _result = error("Unsupported functional type: (Swift.Never) -> Swift.Never")
            run { _result; throw IllegalStateException() }
        }
    }
    val _result = consume_block_with_never(__block)
    return _result
}

@ExportedBridge("__root___consume_block_with_opt_never__TypesOfArguments__U28Swift_Int32_U20Swift_Optional_Swift_Never_U29202D_U20Swift_Int32__")
public fun __root___consume_block_with_opt_never__TypesOfArguments__U28Swift_Int32_U20Swift_Optional_Swift_Never_U29202D_U20Swift_Int32__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Int, arg1: Nothing? ->
            val _arg0 = arg0
            val _arg1 = run { arg1; true }
            val _result = never_type_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_Swift_Never___(closureBox.objcPtr(), _arg0, _arg1)
            _result
        }
    }
    val _result = consume_block_with_opt_never(__block)
    return _result
}
