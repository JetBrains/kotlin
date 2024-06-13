// ISSUE: KT-68975
// KJS_WITH_FULL_RUNTIME
external fun p(s: String, n: () -> String): String

inline fun foo(arg: String, noinline makeString: () -> String): String {
    return js("p(arg, makeString)")
}

fun box() = foo("O") { "K" }
