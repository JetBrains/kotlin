package foo

fun box(): String {
    var boo = "OK"
    var foo = object {
        object bar {
            val baz = boo
        }
    }

    return foo.bar.baz
}

