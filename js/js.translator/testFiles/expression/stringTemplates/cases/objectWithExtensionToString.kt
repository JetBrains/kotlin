package foo

class Foo(val name: String) {
}

public fun Foo.toString(): String {
  return name + "X"
}

fun box(): String {
    val a = Foo("abc")
    val b = Foo("def")
    val message = "a = $a, b = $b"
    return message
}