// ISSUE: KT-68975
// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization
// LANGUAGE: -IrCrossModuleInlinerBeforeKlibSerialization
// See counterpart test with IR inliner in compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/lambdaCrossInline.kt
// KJS_WITH_FULL_RUNTIME
external fun p(s: String, n: () -> String): String

inline fun foo(arg: String, crossinline makeString: () -> String): String {
    return js("p(arg, makeString)")
}

fun box() = foo("O") { "K" }
