// EXPECTED_REACHABLE_NODES: 1294
// LANGUAGE_VERSION: 1.2
// WITH_RUNTIME

fun deinitialize(foo: dynamic) {
  foo.bar = null
}

class Foo {
    @JsName("bar")
    lateinit var bar: String

    fun test(): String {
        if (this::bar.isInitialized) return "Fail 1"
        deinitialize(this)
        if (this::bar.isInitialized) return "Fail 2"

        bar = "A"
        if (!this::bar.isInitialized) return "Fail 3"
        deinitialize(this)
        if (this::bar.isInitialized) return "Fail 4"

        bar = "OK"
        if (!this::bar.isInitialized) return "Fail 5"
        return bar
    }
}

fun box(): String {
    return Foo().test()
}
