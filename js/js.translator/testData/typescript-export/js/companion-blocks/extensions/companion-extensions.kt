// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: JS_TESTS
// FILE: companion-extensions.kt

package foo

@JsExport
class ExportedWithCompanionExtensions

@JsExport
companion fun ExportedWithCompanionExtensions.append(value: String = "K"): String {
    return "O" + value
}
