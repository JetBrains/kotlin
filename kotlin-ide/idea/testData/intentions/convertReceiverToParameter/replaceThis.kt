// WITH_RUNTIME
class A(val s: String) {}

fun <caret>A.extend() = println(this.s)