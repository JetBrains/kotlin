// "Replace with 'p'" "true"
@Deprecated("", ReplaceWith("p"))
fun oldFun(p: Int): Int = p

fun foo() {
    val v = <caret>oldFun(0)
}
