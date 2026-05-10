// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowExportingAnnotationClasses

// MODULE: JS_TESTS
// FILE: annotations.kt

@file:OptIn(ExperimentalVersionOverloading::class)
package foo

@JsExport
annotation class Simple

@Retention(AnnotationRetention.SOURCE)
@JsExport
annotation class WithStringParam(val message: String)

@Retention(AnnotationRetention.BINARY)
@JsExport
annotation class WithMultipleParams(val name: String, val count: Int)

@Retention(AnnotationRetention.RUNTIME)
@JsExport
annotation class WithDefaultValue(val level: Int = 0)

@Target(AnnotationTarget.CLASS)
@JsExport
annotation class WithBooleanParam(val enabled: Boolean)

@JsExport
fun withIntroducedAt(
    x: Int,
    @IntroducedAt("1") y: Int = x,
    @IntroducedAt("1") o1: String = "O",
    @IntroducedAt("1") k: String = "K",
    @IntroducedAt("2") o2: String = o1
): String =
    o2 + k + y.toString()

@JsExport
fun nonAscendingVersion(
    @IntroducedAt("1") y: Int = 42,
    @IntroducedAt("2") o: String = "O",
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> k: String = "K"
) {}

@JsExport
fun invalidParameterPosition(
    x: Int = 4,
    @IntroducedAt("1") y: Int = 2,
    <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>z: Int<!>
) {}

@JsExport
data class ConstructorVersioning(
    val x: Int,
    @IntroducedAt("1") val y: Int = x,
    @IntroducedAt("1") val ok1: String = "OK",
    @IntroducedAt("2") val ok2: String = ok1
)

@JsExport
data class ConstructorNonAscendingVersion(
    val x: Int,
    @IntroducedAt("2") val ok: String = "OK",
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> val y: Int = x
)

@JsExport
data class ConstructorWithInvalidParameterPosition(
    val x: Int,
    @IntroducedAt("1") val y: Int = x,
    <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>val z: Int<!>
)

@JsExport
fun box() = "OK"
