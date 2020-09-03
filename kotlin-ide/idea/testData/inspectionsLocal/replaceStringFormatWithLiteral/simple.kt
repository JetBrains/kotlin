// WITH_RUNTIME

fun test() {
    val foo = 1

    <caret>String.format("foo is %s, bar is %s.", foo, Bar().value)
}

class Bar {
    val value = 2
}