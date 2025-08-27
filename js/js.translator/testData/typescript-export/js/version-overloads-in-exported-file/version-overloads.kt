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

import kotlin.experimental.IntroducedAt


class A (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("1") private val b1: String = "",
    @IntroducedAt("2") val c: Float = 3f,
)

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")

class B (
    val a : Int = 1,
    @IntroducedAt("2") val a1: String = "",
    @IntroducedAt("1") private val b: String = "",
    @IntroducedAt("1") val c: Float = 3f,
)


data class C (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("2") val c: Float = 3f,
)

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")

data class D (
    val a : Int = 1,
    @IntroducedAt("2") val a1: Int = 2,
    @IntroducedAt("1") val b: String = "3",
)


class X {
    fun foo(
        a : Int,
        @IntroducedAt("1") B: String = "",
        @IntroducedAt("1") b1: String = "",
        @IntroducedAt("2") c: Float = 0f,
    ) {}

    @Suppress("NON_ASCENDING_VERSION_ANNOTATION")
    fun mid(
        a : Int,
        @IntroducedAt("2") a1: Int = 1,
        @IntroducedAt("1") b: String = "",
        @IntroducedAt("1") c: Float = 0f,
    ) {}
}


fun foo2(
    a : Int,
    @IntroducedAt("1") B: String = "",
    @IntroducedAt("1") b1: String = "",
    @IntroducedAt("2") c: Float = 0f,
    f: () -> Unit
) {}

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")

fun mid2(
    a : Int,
    @IntroducedAt("2") a1: Int = 1,

    @IntroducedAt("1") b: String = "",
    @IntroducedAt("1") c: Float = 0f,
    f: () -> Unit
) {}