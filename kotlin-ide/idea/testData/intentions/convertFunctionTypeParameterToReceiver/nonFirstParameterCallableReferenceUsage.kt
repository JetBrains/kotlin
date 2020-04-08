// SHOULD_FAIL_WITH: Callable reference transformation is not supported: ::foo
fun foo(f: (Int, <caret>Boolean) -> String) {

}

fun baz(f: (Int, Boolean) -> String) {
    val x = ::foo
}