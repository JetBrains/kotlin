class A {
    companion object {
        const val FOO = 3.14
    }
}

fun main() {
    val a = A.FOO
    print(Int.hashCode())
    print(<warning descr="SSR">a</warning>)
}