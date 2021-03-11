// PROBLEM: none
fun foo(f: (String?) -> Int) {}

fun test() {
    foo {
        if (it != null) return@foo<caret> 1
        0
    }
}