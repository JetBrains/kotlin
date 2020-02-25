// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS
fun foo() {}

class Test {
    val x: String = ""
        get() {
            foo(field<caret>)
            return field
        }
}