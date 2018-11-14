// EXPECTED_REACHABLE_NODES: 1293
package foo

interface Foo {
    fun execute(handler: () -> Unit) {
        execute(false, handler)
    }

    fun execute(onlyIfAttached: Boolean, handler: () -> Unit)
}

object foo : Foo {
    override fun execute(onlyIfAttached: Boolean, handler: () -> Unit) {
        handler()
    }
}

private var result = "fail"

fun box(): String {
    foo.execute() {
        result = "OK"
    }

    return result
}