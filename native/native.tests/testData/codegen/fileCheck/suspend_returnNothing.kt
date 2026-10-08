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
// CHECK-EAGER_SHADOW_STACK-LABEL: define internal ptr @"kfun:$fooCOROUTINE
// CHECK-LATE_SHADOW_STACK-LABEL: define internal ptr addrspace(1) @"kfun:$fooCOROUTINE

// CHECK-NOT: ; Function Attrs: {{.*}}noreturn
// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#foo#suspend(kotlin.coroutines.Continuation<kotlin.Nothing>){}kotlin.Any"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#foo#suspend(kotlin.coroutines.Continuation<kotlin.Nothing>){}kotlin.Any"
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

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
fun box(): String {
    builder {
        bar()
    }
    return "OK"
}

