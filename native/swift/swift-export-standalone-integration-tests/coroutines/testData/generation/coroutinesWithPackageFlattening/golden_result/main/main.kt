@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlinx.coroutines.*

@ExportedBridge("flattened_testSuspendFunction")
public fun flattened_testSuspendFunction(continuation: kotlin.native.internal.NativePtr): Unit {
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Unit>(continuation);
        { arg0: Int ->
            val _result = kotlinFun(arg0)
            _result
        }
    }
    GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
        val _result = flattened.testSuspendFunction()
        __continuation(_result)
    }
}
