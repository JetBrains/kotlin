// LANGUAGE: -ProhibitJsQualifierOnMembersAndNonExternals
// ^^^ Pre-KT-82785 behavior is not broken.
// MODULE: main
// FILE: main.kt
external class C {
    @JsQualifier("a")
    fun o(): String

    @JsQualifier("b")
    class D {
        fun k(): String
    }
}

fun box() = C().o() + C.D().k()
