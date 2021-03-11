fun foo(x: Foo) {
    x.<caret>foo { it * 2 }
}

interface Foo {
    infix fun foo(f: (Int) -> Int)
}
