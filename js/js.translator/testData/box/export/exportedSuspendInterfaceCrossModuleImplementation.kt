// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// ISSUE: KT-86998

// MODULE: caller
// FILE: caller.kt
import kotlin.coroutines.*

external fun setTimeout(handler: () -> Unit, timeout: Int): Int

suspend fun realSuspend(): Unit = suspendCoroutine { continuation ->
    setTimeout({ continuation.resume(Unit) }, 1)
}

@JsExport
interface AsyncService {
    suspend fun compute(): Int
}

@JsExport
suspend fun runService(service: AsyncService): Int = service.compute()

private class SameModuleAsyncServiceImpl(private val value: Int) : AsyncService {
    override suspend fun compute(): Int {
        realSuspend()
        return value
    }
}

@JsExport
fun createSameModuleService(value: Int): AsyncService = SameModuleAsyncServiceImpl(value)

// MODULE: main(caller)
// FILE: main.kt
private class AsyncServiceImpl(private val value: Int) : AsyncService {
    override suspend fun compute(): Int {
        realSuspend()
        return value
    }
}

@JsExport
fun createCrossModuleService(value: Int): AsyncService = AsyncServiceImpl(value)

// FILE: test.js
async function box() {
    const { runService, createSameModuleService, createCrossModuleService } = this.main;
    const expected = 123;

    const sameModuleResult = await runService(createSameModuleService(expected));
    if (sameModuleResult !== expected) return "fail: same module result is " + sameModuleResult;

    const crossModuleResult = await runService(createCrossModuleService(expected));
    if (crossModuleResult !== expected) return "fail: cross module result is " + crossModuleResult;

    return "OK";
}
