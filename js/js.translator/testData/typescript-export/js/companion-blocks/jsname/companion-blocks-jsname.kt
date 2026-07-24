// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: JS_TESTS
// FILE: companion-blocks-jsname.kt

package foo

@JsExport
class WithJsName {
    companion {
        @JsName("renamedFun")
        fun original(): String = "OK"

        @JsName("renamedVal")
        val original: String = "K"
    }
}
