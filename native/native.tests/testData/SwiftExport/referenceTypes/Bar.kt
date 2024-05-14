class Bar(var foo: Foo) {
    fun getAndSetFoo(newFoo: Foo): Foo {
        val oldFoo = foo
        foo = newFoo
        return oldFoo
    }
}

