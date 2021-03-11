// "Create function 'foo'" "false"
// ACTION: Rename reference
// ACTION: Add 'n =' to argument
// ERROR: Unresolved reference: foo
fun bar(n: Int) = "$n"

fun consume(s: String) {}

fun test() {
    consume(bar(::<caret>foo))
}