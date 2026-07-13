// IGNORE_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// WITH_STDLIB
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowExportingSuspendFunctions +JsExportingSuspendLambdas
// MODULE: JS_TESTS
// TSC_TARGET: es2020
// FILE: suspend-lambdas-containers.kt

package foo

@JsExport
class Box<T>(val value: T)

@JsExport
fun produceBoxOfSuspendLambda(): Box<suspend () -> String> =
    Box { "BOX" }

@JsExport
suspend fun callBoxedSuspendLambda(box: Box<suspend () -> String>): String =
    box.value()

@JsExport
fun produceListOfSuspendLambdas(): List<suspend (Int) -> Int> = listOf(
    { x -> x + 3 },
    { x -> x * 4 },
)

@JsExport
suspend fun reduceListOfSuspendLambdas(lambdas: List<suspend (Int) -> Int>, start: Int): Int {
    var acc = start
    for (lambda in lambdas) {
        acc = lambda(acc)
    }
    return acc
}

@JsExport
fun produceMapOfSuspendLambdas(): Map<String, suspend () -> String> = mapOf(
    "ok" to { "OK" },
    "value" to { "VALUE" },
)

@JsExport
suspend fun callMapOfSuspendLambdas(lambdas: Map<String, suspend () -> String>, key: String): String? =
    lambdas[key]?.invoke()
