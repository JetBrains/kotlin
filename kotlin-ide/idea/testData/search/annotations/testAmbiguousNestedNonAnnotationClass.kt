package foo

annotation class Xyzzy

object Foo {
    class Xyzzy
}

@Xyzzy fun foo()

// ANNOTATION: foo.Xyzzy
// SEARCH: method:foo
// OPTIMIZED_TRUE: 1
