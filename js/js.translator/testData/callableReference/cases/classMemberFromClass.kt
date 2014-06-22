package foo

class A {
    fun bar(k: Int) = k

    fun result() = this.(::bar)(111)
}

fun box(): String {
    val result = A().result()
    if (result != 111) return "Fail $result"
    return "OK"
}
