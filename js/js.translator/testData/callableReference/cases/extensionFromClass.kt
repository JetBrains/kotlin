package foo

class A {
    fun result() = this.(::bar)("OK")
}

fun A.bar(x: String) = x

fun box() = A().result()
