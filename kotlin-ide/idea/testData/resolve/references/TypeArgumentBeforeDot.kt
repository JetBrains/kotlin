class Generic<T> {
    class Nested
}

class C {
    val prop: Generic<<caret>Foo>.Nested? = null
}

class Foo

// REF: (<root>).Foo
