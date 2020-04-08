// "Change type from 'String' to '(Int) -> String'" "true"
fun foo(f: ((Int) -> String) -> String) {
    foo {
        f: String<caret> -> f(42)
    }
}