infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

fun foo() {
    val pair = 1 to<caret>
}
