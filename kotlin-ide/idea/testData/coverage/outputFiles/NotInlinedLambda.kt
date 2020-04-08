package org.demo.coverage

public class Foo {
    public fun forEach(fn: (Any?) -> Unit): Unit {
        fn(1)
    }

    public fun bar() {
        forEach {
            println("foo")
        }
    }
}
