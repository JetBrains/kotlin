package foo

abstract class A(val s: String) {
}

object B : A("test") {
}

fun box() = B.s == "test"
