@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch as kotlinx_coroutines_launch

@ExportedBridge("flattened_testSuspendFunction")
public fun flattened_testSuspendFunction(continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Boolean>(continuation);
        { arg0: Int ->
            val _arg0 = arg0
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        flattened.testSuspendFunction()
    }
}
