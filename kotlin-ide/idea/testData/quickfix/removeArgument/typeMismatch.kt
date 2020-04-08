// "Remove argument" "false"
// ACTION: Add 'toString()' call
// ACTION: Change parameter 't' type of function 'foo' to 'Int'
// ACTION: Convert to also
// ACTION: Convert to apply
// ACTION: Convert to run
// ACTION: Convert to with
// ACTION: Put arguments on separate lines
// ERROR: The integer literal does not conform to the expected type String
class Bar() {
    fun foo(s: String, vararg t: String) {
    }
}

fun main() {
    val b = Bar()
    b.foo("a", "b", 1<caret>)
}