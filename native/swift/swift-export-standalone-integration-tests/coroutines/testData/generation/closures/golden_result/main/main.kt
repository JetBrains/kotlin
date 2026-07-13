@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___accept_suspend_fun_with_context__TypesOfArguments__U28Swift_StringU2920asyncU20throwsU202D_U20Swift_Int32__")
public fun __root___accept_suspend_fun_with_context__TypesOfArguments__U28Swift_StringU2920asyncU20throwsU202D_U20Swift_Int32__(block: kotlin.native.internal.NativePtr): Int {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->Boolean>(block);
        suspend { ctx0: kotlin.String ->
            suspendSwiftCoroutine { continuation: Function1<Int, Unit>, exception: Function1<platform.Foundation.NSError?, Unit>, cancellation: SwiftJob ->
                val _ctx0 = ctx0.objcPtr()
                val _continuation = kotlin.native.internal.ref.createRetainedExternalRCRef(continuation)
                val _exception = kotlin.native.internal.ref.createRetainedExternalRCRef(exception)
                val _cancellation = kotlin.native.internal.ref.createRetainedExternalRCRef(cancellation)
                val _result = kotlinFun(_ctx0, _continuation, _exception, _cancellation)
                run<Unit> { _result }
            }
        }
    }
    val _result = run { accept_suspend_fun_with_context(__block) }
    return _result
}

@ExportedBridge("__root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__")
public fun __root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__(block: kotlin.native.internal.NativePtr): Int {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->Boolean>(block);
        suspend {
            suspendSwiftCoroutine { continuation: Function1<Int, Unit>, exception: Function1<platform.Foundation.NSError?, Unit>, cancellation: SwiftJob ->
                val _continuation = kotlin.native.internal.ref.createRetainedExternalRCRef(continuation)
                val _exception = kotlin.native.internal.ref.createRetainedExternalRCRef(exception)
                val _cancellation = kotlin.native.internal.ref.createRetainedExternalRCRef(cancellation)
                val _result = kotlinFun(_continuation, _exception, _cancellation)
                run<Unit> { _result }
            }
        }
    }
    val _result = run { accept_suspend_function_type(__block) }
    return _result
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Int): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val _result = run { (__pointerToBlock as Function1<Int, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = if (_1 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<platform.Foundation.NSError>(_1)
    val _result = run { (__pointerToBlock as Function1<platform.Foundation.NSError?, Unit>).invoke(___1) }
    return run { _result; true }
}
