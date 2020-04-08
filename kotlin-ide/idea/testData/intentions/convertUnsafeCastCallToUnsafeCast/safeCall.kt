// JS
// IS_APPLICABLE: false
fun test(foo: Any?) {
    val s = <caret>foo?.unsafeCast<String>()
}