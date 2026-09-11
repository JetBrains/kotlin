// CHECK_OPTIMIZED_JS
// WITH_STDLIB

// FILE: keepEscape.kt

import kotlin.reflect.KFunction0

// Each case observes KFunction metadata through a flow the DFG cannot follow precisely
// (vararg elements, closure captures, indirect invocations). The demand analysis must
// treat such escapes as keep-sinks; every case below breaks at runtime if its wrapper
// is unwrapped, so plain box assertions are enough — no generated-code expectations.

fun capturedName() {}

fun assignedInLambda() {}

fun arrayName() {}

fun listName() {}

fun invokeArg() {}

fun fromLambda() {}

fun viaRef() {}

// Shares the `invoke` name but is not FunctionN.invoke: its argument's metadata is observed.
fun invoke(k: KFunction0<Unit>): String = k.name

fun makeRef(): KFunction0<Unit> = ::viaRef

fun openInvokeArg() {}

// A user `invoke` that is open: the declared body is benign, but a virtual call may dispatch
// to an override that observes the argument's metadata.
open class CallerBase {
    open operator fun invoke(k: KFunction0<Unit>): String = "base"
}

class CallerSub : CallerBase() {
    override fun invoke(k: KFunction0<Unit>): String = k.name
}

fun callThrough(c: CallerBase) = c(::openInvokeArg)

fun providedName() {}

// Overrides an external member: external JS can call `provide` without any call site in the graph.
external interface ExtProvider {
    fun provide(): KFunction0<Unit>
}

class ExtProviderImpl : ExtProvider {
    override fun provide(): KFunction0<Unit> = ::providedName
}

external fun readProvidedName(p: ExtProvider): String

fun box(): String {
    val captured = ::capturedName
    val readCaptured = { captured.name }
    if (readCaptured() != "capturedName") return "fail captured"

    var assigned: KFunction0<Unit>? = null
    val doAssign = { assigned = ::assignedInLambda }
    doAssign()
    if (assigned?.name != "assignedInLambda") return "fail assigned"

    val arr: Array<KFunction0<Unit>> = arrayOf(::arrayName)
    if (arr[0].name != "arrayName") return "fail array"
    if (listOf<KFunction0<Unit>>(::listName).single().name != "listName") return "fail list"

    if (invoke(::invokeArg) != "invokeArg") return "fail invoke"

    val make = { ::fromLambda }
    if (make().name != "fromLambda") return "fail lambda"

    val g = ::makeRef
    if (g().name != "viaRef") return "fail via ref"

    if (callThrough(CallerSub()) != "openInvokeArg") return "fail open invoke"

    if (readProvidedName(ExtProviderImpl()) != "providedName") return "fail external override"

    return "OK"
}

// FILE: keepEscape_ext.js
function readProvidedName(p) {
    return p.provide().callableName;
}
