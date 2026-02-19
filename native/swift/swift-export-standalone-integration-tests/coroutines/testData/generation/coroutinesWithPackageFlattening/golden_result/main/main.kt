@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlinx.coroutines.*

@ExportedBridge("flattened_testSuspendFunction")
public fun flattened_testSuspendFunction(continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Unit>(continuation);
        { arg0: Int ->
            val _result = kotlinFun(arg0)
            _result
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = flattened.testSuspendFunction()
            __continuation(_result)
        } catch (error: CancellationException) {
            __cancellation.cancel()
            __exception(null)
            throw error
        } catch (error: Throwable) {
            __exception(error)
        }
    }.alsoCancel(__cancellation)
}
