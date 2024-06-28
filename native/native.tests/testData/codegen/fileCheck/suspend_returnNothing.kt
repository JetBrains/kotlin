// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun suspendForever(): Int = suspendCoroutineUninterceptedOrReturn {
    COROUTINE_SUSPENDED
}
// CHECK-LABEL: define internal %struct.ObjHeader* @"kfun:$fooCOROUTINE

// CHECK-NOT: ; Function Attrs: {{.*}}noreturn
// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#foo#suspend(kotlin.coroutines.Continuation<kotlin.Nothing>){}kotlin.Any"
suspend fun foo(): Nothing {
    suspendForever()
    throw Error()
}

suspend fun bar() {
    foo()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    builder {
        bar()
    }
    return "OK"
}

