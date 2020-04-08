// "Change to 'return@init'" "false"
// ACTION: Introduce local variable
// ERROR: 'return' is not allowed here
// WITH_RUNTIME

class Foo {
    init {
        return<caret> 1
    }
}