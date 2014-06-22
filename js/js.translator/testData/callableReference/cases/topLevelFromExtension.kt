package foo

fun tmp(o: Int, k: Int) = o + k

class A

fun A.bar() = (::tmp)(111, 222)

fun box(): String {
    val result = A().bar()
    if (result != 333) return "Fail $result"
    return "OK"
}
