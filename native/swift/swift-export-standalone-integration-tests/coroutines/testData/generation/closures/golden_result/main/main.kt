@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__")
public fun __root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__(block: kotlin.native.internal.NativePtr): Int {
    val __block = run {
        val originalBlock = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->Unit>(block);
        suspend {
            val __cancellation: SwiftJob = SwiftJob()
            kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.let {
                __cancellation.alsoCancel(it)
                it.alsoCancel(__cancellation)
            }

            kotlinx.coroutines.suspendCancellableCoroutine { __cont ->
                val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
                val __continuation: Function1<Int, Unit> = { _result ->
                    if (__cont.isActive) __cont.resumeWith(kotlin.Result.success(_result))
                }
                val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
                val __exception: Function1<platform.Foundation.NSError, Unit> = { _error ->
                    if (__cont.isActive) __cont.resumeWith(kotlin.Result.failure(SwiftException(_error)))
                }
                val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
                originalBlock(__continuationPtr, __exceptionPtr, __cancellationPtr)
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

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = interpretObjCPointer<platform.Foundation.NSError>(_1)
    val _result = run { (__pointerToBlock as Function1<platform.Foundation.NSError, Unit>).invoke(___1) }
    return run { _result; true }
}
