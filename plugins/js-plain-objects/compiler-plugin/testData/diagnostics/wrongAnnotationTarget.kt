// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.js.JsPlainObject

<!NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED("class")!>@JsPlainObject
class Regular1<!>

@JsPlainObject
<!NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED("object")!>object Regular2<!>

<!NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED("enum class")!>@JsPlainObject
enum class Regular3<!>

<!ONLY_INTERFACES_ARE_SUPPORTED("class")!>@JsPlainObject
external class External1<!>

@JsPlainObject
external <!ONLY_INTERFACES_ARE_SUPPORTED("object")!>object External2<!>

@JsPlainObject
external interface External3

external class Nested {
    @JsPlainObject
    interface Inner
}