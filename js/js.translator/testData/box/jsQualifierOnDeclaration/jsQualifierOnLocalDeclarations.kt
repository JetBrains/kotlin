// MODULE: main
// FILE: main.kt
// Should become red when KT-82785 is fixed
external class C {
    @JsQualifier("a")
    fun o(): String

    @JsQualifier("b")
    class D {
        fun k(): String
    }
}

fun box() = C().o() + C.D().k()