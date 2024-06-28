
class Receiver

fun foo() = with(Receiver()) {
    bar()
    baz()
}

fun bar() {}

fun Receiver.baz() {}
