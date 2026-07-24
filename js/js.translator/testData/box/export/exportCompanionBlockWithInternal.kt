// RUN_PLAIN_BOX_FUNCTION
// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// FILE: lib.kt
@JsExport
class Test {
    companion {
        internal val secret: String = "secret"
        val publicVal: String = "public"

        internal fun secretFun(): String = "secretFun"
        fun publicFun(): String = "publicFun"
    }
}

@JsExport
fun useInternals(): String = Test.secret + "|" + Test.secretFun()

// FILE: main.js
function box() {
    var Test = this.lib.Test;

    if (Test.publicVal !== "public") return "FAIL: public val"
    if (Test.publicFun() !== "publicFun") return "FAIL: public fun"
    if (this.lib.useInternals() !== "secret|secretFun") return "FAIL: internal access"

    if (typeof Test.secret !== "undefined") return "FAIL: internal val leaked to JS"
    if (typeof Test.secretFun !== "undefined") return "FAIL: internal fun leaked to JS"

    return "OK"
}
