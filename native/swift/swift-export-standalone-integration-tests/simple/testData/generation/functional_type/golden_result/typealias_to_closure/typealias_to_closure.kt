@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___typealias_demo__TypesOfArguments__U28Swift_Int32_U20Swift_Int32U29202D_U20Swift_Void__")
public fun __root___typealias_demo__TypesOfArguments__U28Swift_Int32_U20Swift_Int32U29202D_U20Swift_Void__(input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __input = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int, Int)->Unit>(input);
        { arg0: Int, arg1: Int ->
            val _result = kotlinFun(arg0, arg1)
            Unit
        }
    }
    val _result = typealias_demo(__input)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealias_to_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__")
public fun typealias_to_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Int, _2: Int): Unit {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val ___2 = _2
    (__pointerToBlock as Function2<Int, Int, Unit>).invoke(___1, ___2)
}
