// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// MODULE: lib
// MODULE_KIND: ES
// ES_MODULES
// SPLIT_PER_MODULE
// TSC_MODULE: ESNext
// FILE: js_static.kt

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

        // KT-85990
        @JsStatic
        suspend fun staticSuspendWithDefault(value: String = "DEFAULT STATIC SUSPEND"): String = value

        suspend fun companionSuspendWithDefault(value: String = "DEFAULT COMPANION SUSPEND"): String = value
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

        // KT-85990
        @JsStatic
        suspend fun staticSuspendWithDefault(value: String = "DEFAULT STATIC SUSPEND"): String = value

        suspend fun companionSuspendWithDefault(value: String = "DEFAULT COMPANION SUSPEND"): String = value
    }
}

@JsExport
object ObjectWithJsStatic {
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

    // KT-85990
    @JsStatic
    suspend fun staticSuspendWithDefault(value: String = "DEFAULT STATIC SUSPEND"): String = value

    suspend fun companionSuspendWithDefault(value: String = "DEFAULT COMPANION SUSPEND"): String = value
}
