// PROBLEM: none
fun foo(p: List<String?>): Int? {
    val v = p[0]
    <caret>if (v == null) bar()
    return v?.length
}

fun bar(){}