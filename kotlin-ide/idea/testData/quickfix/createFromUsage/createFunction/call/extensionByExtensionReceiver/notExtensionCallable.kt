// "Create extension function 'foo'" "false"
// ACTION: Create function 'foo'
// ACTION: Rename reference
// ACTION: Add 'b =' to argument
// ERROR: Unresolved reference: foo
fun bar(b: Boolean) {

}

fun test() {
    bar(<caret>foo(1))
}