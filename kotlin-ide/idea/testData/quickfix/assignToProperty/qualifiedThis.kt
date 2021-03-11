// "Assign to property" "true"
// WITH_RUNTIME
class Test {
    var foo = 1

    fun test(foo: Int) {
        "".run {
            <caret>foo = foo
        }
    }
}