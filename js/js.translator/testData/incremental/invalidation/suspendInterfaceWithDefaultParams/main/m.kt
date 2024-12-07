import kotlin.coroutines.*

internal suspend fun testDefaltParam(stepId: Int): Int {
    return callFun(ClassA2())
}

private suspend fun ignoreIt() = ClassA1()

private suspend fun callFun(a: InterfaceA): Int {
    return a.functionA(0, "", false)
}

suspend fun suspendBox(stepId: Int): String {
    if (testDefaltParam(stepId) != stepId) {
        return "Fail"
    }
    return "OK"
}

fun runCoroutine(coroutine: suspend () -> String): String {
    var result: String = "Uninitialized"
    coroutine.startCoroutine(object : Continuation<String> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(r: Result<String>) {
            result = r.getOrThrow()
        }
    })
    return result
}

fun box(stepId: Int, isWasm: Boolean) = runCoroutine { suspendBox(stepId) }