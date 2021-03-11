// "Make 'Foo' data class" "false"
// ACTION: Create extension function 'Test.Foo.component1'
// ACTION: Create extension function 'Test.Foo.component2'
// ACTION: Create member function 'Test.Foo.component1'
// ACTION: Create member function 'Test.Foo.component2'
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Put arguments on separate lines
// ERROR: Destructuring declaration initializer of type Test.Foo must have a 'component1()' function
// ERROR: Destructuring declaration initializer of type Test.Foo must have a 'component2()' function
class Test {
    inner class Foo(val bar: String, val baz: Int)
    fun test() {
        var (bar, baz) = Foo("A", 1)<caret>
    }
}