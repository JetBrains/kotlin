// FIR_COMPARISON
fun main(foo: Foo) {
    foo.apply {
        is<caret>
    }
}
class Foo(val isValid: String)

// EXIST: isValid