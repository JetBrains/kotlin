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

        @JsStatic
        suspend fun staticSuspendWithSeveralDefaults(prefix: String = "A", count: Int = 2, suffix: String = "Z"): String =
            "$prefix$count$suffix"

        @JsStatic
        suspend fun staticSuspendWithRequiredAndDefault(required: String, optional: String = "D"): String =
            "$required$optional"

        @JsStatic
        suspend fun staticSuspendWithDependentDefault(prefix: String = "A", suffix: String = prefix + "Z"): String =
            "$prefix$suffix"

        @JsStatic
        suspend fun staticSuspendWithReceiverDefault(value: String = hidden()): String = value

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

        @JsStatic
        @JsName("renamedStaticSuspendDefault")
        suspend fun staticSuspendWithJsNameDefault(value: String = "RENAMED DEFAULT"): String = value

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
