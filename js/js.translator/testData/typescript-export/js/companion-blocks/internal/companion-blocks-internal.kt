// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: JS_TESTS
// FILE: companion-blocks-internal.kt

package foo

@JsExport
class WithInternal {
    companion {
        private val privateVal: String = "private"
        internal val secret: String = "x"
        val publicVal: String = "y"

        private fun privateFun(): String = privateVal
        internal fun secretFun(): String = secret
        fun publicFun(): String = publicVal
    }
}
