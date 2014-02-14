package foo

trait Foo {
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

private var result = false

fun box(): Boolean {
    foo.execute() {
        result = true
    }

    return result
}