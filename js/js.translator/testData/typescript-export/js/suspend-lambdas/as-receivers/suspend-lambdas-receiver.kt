// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowExportingSuspendFunctions +JsExportingSuspendLambdas
// MODULE: JS_TESTS
// TSC_TARGET: es2020
// FILE: suspend-lambdas-receiver.kt

package foo

@JsExport
val receiverSuspendLambda: suspend String.(Int) -> String = { x -> this + x.toString() }

@JsExport
suspend fun callReceiverSuspendLambda(
    callback: suspend String.(Int) -> String,
    receiver: String,
    x: Int
): String = receiver.callback(x)

@JsExport
val nullableReceiverSuspendLambda: suspend String?.() -> String = { this ?: "null-receiver" }

@JsExport
suspend fun callNullableReceiverLambda(
    cb: suspend String?.() -> String,
    receiver: String?
): String = receiver.cb()

@JsExport
class Receiver(val v: Int)

@JsExport
val classReceiverSuspendLambda: suspend Receiver.() -> Int = { v * 2 }

@JsExport
suspend fun callClassReceiverLambda(
    cb: suspend Receiver.() -> Int,
    receiver: Receiver
): Int = receiver.cb()
