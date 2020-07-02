// "Add '== true'" "false"
// DISABLE-ERRORS
// ACTION: Add 'toString()' call
// ACTION: Change parameter 's' type of function 'baz' to 'Boolean?'
// ACTION: Create function 'baz'
class Foo {
    fun bar() = true
}

fun baz(s: String) {}

fun test(foo: Foo?) {
    baz(foo?.bar()<caret>)
}
