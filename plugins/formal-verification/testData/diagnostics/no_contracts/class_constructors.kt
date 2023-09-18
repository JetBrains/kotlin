class Foo {
    val a: Int
    constructor(n: Int) {
        this.a = n
    }
    constructor(b: Boolean) {
        this.a = when (b) {
            true -> 1
            false -> 0
        }
    }

    constructor(x1: Int, x2: Int) {
        a = x1 + x2
    }
}

class Bar(val a: Int) {
    constructor(b: Boolean) : this(if (b) { 1 } else { 0 }) {
        // Do something...
    }
}

fun <!VIPER_TEXT!>onlySecondConstructors<!>() {

    val f1 = Foo(true)
    val shouldBeOne = f1.a // 1

    val f2 = Foo(42)
    val shouldBeAnswer = f2.a // 42

    val f3 = Foo(10, 32)
    val test = f3.a // 42

}

fun <!VIPER_TEXT!>primaryAndSecondConstructor<!>() {
    val b1 = Bar(false)
    val shouldBeZero = b1.a // 0

    val b2 = Bar(42)
    val shouldBeAnswer = b2.a // 42
}