// WITH_RUNTIME
// PROBLEM: none

fun foo(s: String, i: Int) = s.length + i

fun test() {
    val s = ""
    s.let<caret> { foo(it, it.length) }
}