// "Create extension property 'T.bar'" "true"
fun consume(n: Int) {}

fun <T> foo(t: T) {
    consume(t.<caret>bar)
}