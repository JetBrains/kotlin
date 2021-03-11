package test

inline fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()

class A() {
    fun X.bar() {}
}

class X {
    fun Int.foo() {}

}