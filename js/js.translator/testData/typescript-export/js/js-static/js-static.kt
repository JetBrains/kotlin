// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: regular-classes.kt

package foo

@JsExport
class WithIgnoredCompanion {
    // TODO: we should discuss this moment
    @JsExport.Ignore
    companion object {
        @JsStatic
        @JsName("bar")
        fun pep(): String = hidden()

        fun hidden(): String = "BARRRR"

        @JsStatic
        val foo = "FOOOO"

        @JsStatic
        val baz get() = delegated

        val delegated = "BAZZZZ"

        @JsStatic
        var mutable = "INITIAL"

        @JsStatic
        suspend fun staticSuspend(): String = "STATIC SUSPEND"

        suspend fun companionSuspend(): String = "SUSPEND"
    }
}

@JsExport
class WithoutIgnoredCompanion {
    companion object {
        @JsStatic
        @JsName("bar")
        fun pep(): String = hidden()

        fun hidden(): String = "BARRRR"

        @JsStatic
        val foo = "FOOOO"

        @JsStatic
        val baz get() = delegated

        val delegated = "BAZZZZ"

        @JsStatic
        var mutable = "INITIAL"

        @JsStatic
        suspend fun staticSuspend(): String = "STATIC SUSPEND"

        suspend fun companionSuspend(): String = "SUSPEND"
    }
}
