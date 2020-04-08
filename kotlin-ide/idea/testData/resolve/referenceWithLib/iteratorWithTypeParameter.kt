package test

import dependency.*

fun foo(f: Foo<Int>) {
    for (elem i<caret>n f) {
    }
}

// MULTIRESOLVE
// REF: (for dependency.Foo<T> in dependency).iterator()
// REF: (for dependency.FooIterator<T> in dependency).hasNext()
// REF: (for dependency.FooIterator<T> in dependency).next()
