// PROBLEM: 'copy' method of data class is called without named arguments

data class Foo(val a: String)

fun bar(f: Foo) {
    f.co<caret>py("")
}