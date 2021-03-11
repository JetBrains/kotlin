interface T {
    fun foo(vararg a: Any): T = this
}

fun foo(t: T) {
    t.foo(
            1 + 2,
            <caret>2, 3
    ).foo()
            .foo(4)
}

/*
2
t.foo(...)
t.foo(...).foo()
t.foo(...).foo().foo(...)
*/