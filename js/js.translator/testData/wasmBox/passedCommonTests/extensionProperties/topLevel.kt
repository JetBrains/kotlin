val Int.foo: String
        get() = "OK"

fun box(): String {
    return 1.foo
}
