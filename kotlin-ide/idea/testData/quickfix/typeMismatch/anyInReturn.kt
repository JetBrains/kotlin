// "Change return type of enclosing function 'foo' to 'Any'" "true"
fun foo() {
    class A

    return <caret>A()
}