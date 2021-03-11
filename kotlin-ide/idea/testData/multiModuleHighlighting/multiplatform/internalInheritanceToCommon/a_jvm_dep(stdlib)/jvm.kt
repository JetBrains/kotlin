package foo

private class A : CommonAbstract() {
    override fun foo(cause: Throwable?) {}
}

internal actual abstract class ExpectBase actual constructor() : I {
    actual abstract override fun foo(cause: Throwable?)
}
