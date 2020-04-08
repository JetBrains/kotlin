// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: NONE_APPLICABLE
// !MESSAGE_TYPE: HTML

fun foo(i: Int, s: String, b: Boolean) {}
fun foo(i: Int, s: Int) {}
fun foo(b: Boolean, s: String) {}
fun foo(i: Int) {}

fun test() {
    foo(1, "")
}
