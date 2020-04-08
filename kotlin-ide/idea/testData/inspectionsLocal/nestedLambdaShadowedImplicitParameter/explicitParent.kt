// PROBLEM: none

fun foo(f: (String) -> Unit) {}
fun bar(s: String) {}

fun test() {
    foo { s ->
        foo {
            bar(it<caret>)
        }
    }
}