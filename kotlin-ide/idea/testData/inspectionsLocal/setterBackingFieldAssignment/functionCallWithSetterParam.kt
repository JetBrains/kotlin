// PROBLEM: none
class Test {
    var foo: Int = 10
        <caret>set(value) {
            bar(value)
        }

    fun bar(value: Int) {}
}