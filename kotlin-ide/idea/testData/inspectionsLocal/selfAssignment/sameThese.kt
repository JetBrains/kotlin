// PROBLEM: Variable 'foo' is assigned to itself
// FIX: Remove self assignment

class Test {
    var foo = 1

    fun test() {
        this.foo = <caret>this@Test.foo
    }
}