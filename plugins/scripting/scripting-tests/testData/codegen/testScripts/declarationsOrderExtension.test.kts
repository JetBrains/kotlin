
class Receiver

fun foo() {
    Receiver().baz()
}

fun Receiver.foo() {
    bar()
    baz()
}

fun bar() {}

fun Receiver.baz() {}
