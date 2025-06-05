// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// DISABLE_IR_VISIBILITY_CHECKS: ANY
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: version-overloads.kt

@file:JsExport
@file:OptIn(ExperimentalStdlibApi::class)
package foo


class A (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("1") private val b1: String = "",
    @IntroducedAt("2") val c: Float = 3f,
)


data class C (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("2") val c: Float = 3f,
)


class X {
    fun foo(
        a : Int,
        @IntroducedAt("1") B: String = "",
        @IntroducedAt("1") b1: String = "",
        @IntroducedAt("2") c: Float = 0f,
    ) {}
}


fun foo2(
    a : Int,
    @IntroducedAt("1") B: String = "",
    @IntroducedAt("1") b1: String = "",
    @IntroducedAt("2") c: Float = 0f,
    f: () -> Unit
) {}