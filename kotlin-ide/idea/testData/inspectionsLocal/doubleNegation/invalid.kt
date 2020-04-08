// PROBLEM: none
operator fun Int.not() = this * -1
fun foo() {
    val c = !!<caret>1
}