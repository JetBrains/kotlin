package foo

class A {
    var result = "OK"
}

fun box() = (::A)().result
