fun foo(a: Int, b: Int) = a + b

fun test(): String {
    return "Foo: ${foo(0, 4).<caret>toString()}"
}