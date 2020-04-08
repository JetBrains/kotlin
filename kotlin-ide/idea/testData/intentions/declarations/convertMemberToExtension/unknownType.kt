// ERROR: A 'return' expression required in a function with a block body ('{...}')
// ERROR: Unresolved reference: Foo
// ERROR: Unresolved reference: bar

class Owner {
    fun <caret>f(p: Foo): bar.Baz {
    }
}
