// ISSUE: KT-68975
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: IllegalStateException: FAILOK
// REASON: No sane way to codegen this snippet
// Reason will be changed after KT-66181, when `js()` would be forbidden to use inlined lambdas
// KJS_WITH_FULL_RUNTIME
external fun p(s: String, n: () -> String): String

inline fun foo(arg: String, makeString: () -> String): String {
    return js("p(arg, makeString)")
}

fun box(): String {
    val mustBeUnreachable = foo("FAIL") {
        return "OK"
    }
    error(mustBeUnreachable)
}
