// "Create member property 'bar'" "false"
// ACTION: Create extension property 'T.bar'
// ACTION: Rename reference
// ACTION: Add 'n =' to argument
// ERROR: Unresolved reference: bar
fun consume(n: Int) {}

fun <T> foo(t: T) {
    consume(t.<caret>bar)
}