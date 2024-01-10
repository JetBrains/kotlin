// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.jso.JsSimpleObject

<!NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED("class")!>@JsSimpleObject
class Regular1<!>

@JsSimpleObject
<!NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED("object")!>object Regular2<!>

<!NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED("enum class")!>@JsSimpleObject
enum class Regular3<!>

<!ONLY_INTERFACES_ARE_SUPPORTED("class")!>@JsSimpleObject
external class External1<!>

@JsSimpleObject
external <!ONLY_INTERFACES_ARE_SUPPORTED("object")!>object External2<!>

@JsSimpleObject
external interface External3

external class Nested {
    @JsSimpleObject
    interface Inner
}