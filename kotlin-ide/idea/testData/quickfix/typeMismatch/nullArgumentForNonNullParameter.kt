// "Change parameter 's' type of primary constructor of class 'Foo' to 'String?'" "true"
class Foo(s: String) {

}

fun test() {
    Foo(<caret>null)
}