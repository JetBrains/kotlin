
class Receiver

fun foo() {
    bar()
    Receiver().baz()
}

fun Receiver.foo() {
    bar()
    baz()
}

// Could be deprecated later
bar()

fun bar() {}

fun Receiver.baz() {}
