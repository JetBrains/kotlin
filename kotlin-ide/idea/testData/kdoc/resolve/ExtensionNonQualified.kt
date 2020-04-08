class Foo

fun Foo.foo(){}

/**
 * [<caret>foo]
 */
fun test() {}

// REF: (for Foo in <root>).foo()
