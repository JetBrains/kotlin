class Foo {
    /**
     * [b<caret>]
     */
    fun baz() {

    }

    fun bar() {

    }
}

fun Foo.boo() {
}

// EXIST: bar
// EXIST: baz
// EXIST: boo
