// IGNORE_BACKEND: JS_IR
// IGNORE_ANALYSIS_API_BASED_TYPESCRIPT_EXPORT: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowExportingSuspendFunctions +JsExportingSuspendLambdas
// MODULE: JS_TESTS
// TSC_TARGET: es2020
// FILE: suspend-lambdas.kt

package foo

private fun assert(condition: Boolean) {
    if (!condition) {
        throw Throwable("Unexpected behavior")
    }
}

@JsExport
val exportedSuspendLambda: suspend () -> String = { "OK" }

@JsExport
fun produceSuspendLambda(): suspend (Int) -> Int = { x -> x * 2 }

@JsExport
fun produceCapturingSuspendLambda(base: Int): suspend (Int) -> Int = { x -> x + base }

@JsExport
suspend fun runLambda(callback: suspend (Int) -> Int): Int = callback(21) * 2

@JsExport
suspend fun runVoidLambda(callback: suspend () -> Unit) {
    callback()
}

@JsExport
suspend fun chain(
    a: suspend (Int) -> Int,
    b: suspend (Int) -> Int,
    x: Int
): Int = b(a(x))

@JsExport
fun roundTrip(callback: suspend (Int) -> Int): suspend (Int) -> Int =
    { x -> callback(x) + 1 }

@JsExport
fun <T> genericRoundTrip(callback: suspend (T) -> T): suspend (T) -> T =
    { x -> callback(x) }

@JsExport
val nullableSuspendLambda: (suspend () -> String)? = { "nullable" }

@JsExport
suspend fun callNullableSuspendLambda(callback: (suspend (Int) -> String)?, x: Int): String? =
    callback?.invoke(x)

@JsExport
class LambdaHolder(private val base: Int) {
    val multiplier: suspend (Int, Int) -> Int = { x, y -> x * y }

    fun produceAdder(): suspend (Int) -> Int = { x -> x + base }

    suspend fun apply(cb: suspend (Int) -> Int, x: Int): Int = cb(x)

    suspend fun applyTwice(cb: suspend (Int) -> Int, x: Int): Int = cb(cb(x))
}

@JsExport
suspend fun callKotlinLambdaFromKotlin(): Int {
    val lambda = produceSuspendLambda()
    return lambda(5)
}

// Arrays of suspend lambdas

@JsExport
fun produceArrayOfSuspendLambdas(): Array<suspend (Int) -> Int> = arrayOf(
    { x -> x + 1 },
    { x -> x * 2 },
    { x -> x * x },
)

@JsExport
suspend fun reduceArrayOfSuspendLambdas(lambdas: Array<suspend (Int) -> Int>, start: Int): Int {
    var acc = start
    for (lambda in lambdas) {
        acc = lambda(acc)
    }
    return acc
}

@JsExport
suspend fun mapWithArrayOfSuspendLambdas(
    lambdas: Array<suspend (Int) -> Int>,
    x: Int
): Array<Int> = Array(lambdas.size) { lambdas[it](x) }

// Callable references to suspend functions exposed as suspend lambdas

private suspend fun privateSuspendDouble(x: Int): Int = x * 2

@JsExport
suspend fun topLevelSuspendInc(x: Int): Int = x + 1

@JsExport
fun getSuspendDoubleRef(): suspend (Int) -> Int = ::privateSuspendDouble

@JsExport
fun getSuspendIncRef(): suspend (Int) -> Int = ::topLevelSuspendInc

@JsExport
class WithSuspendMethod(private val delta: Int) {
    suspend fun addDelta(x: Int): Int = x + delta
    fun memberRef(): suspend (Int) -> Int = ::addDelta
}

// Interface and abstract class with suspend lambda properties for TypeScript override

@JsExport
@JsNoRuntime
interface InterfaceWithSuspendLambdaProp {
    val handler: suspend (Int) -> String
}

@JsExport
abstract class AbstractClassWithSuspendLambdaProp {
    abstract val handler: suspend (Int) -> String
}

@JsExport
suspend fun callHandlerFromInterface(holder: InterfaceWithSuspendLambdaProp, x: Int): String =
    holder.handler(x)

@JsExport
suspend fun callHandlerFromAbstractClass(holder: AbstractClassWithSuspendLambdaProp, x: Int): String =
    holder.handler(x)

@JsExport
suspend fun callbackThatThrows(callback: suspend () -> String): String =
    try {
        callback()
    } catch (e: Throwable) {
        "caught:" + (e.message ?: "<no-message>")
    }

@JsExport
suspend fun throwingSuspendLambda(): String =
    throw Throwable("boom")

@JsExport
suspend fun applyAll(start: Int, vararg callbacks: suspend (Int) -> Int): Int {
    var acc = start
    for (cb in callbacks) acc = cb(acc)
    return acc
}

@JsExport
suspend fun withDefaultCallback(x: Int, cb: suspend (Int) -> Int = { it + 100 }): Int = cb(x)

@JsExport
fun produceNestedSuspendLambda(): suspend () -> suspend () -> String = { { "NESTED" } }

@JsExport
suspend fun callNestedSuspendLambda(maker: suspend () -> suspend () -> String): String = maker()()
