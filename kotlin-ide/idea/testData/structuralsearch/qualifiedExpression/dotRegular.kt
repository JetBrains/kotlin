class A {
    companion object {
        const val FOO = 3.14
    }
}

fun main() {
    val a = <warning descr="SSR">A.FOO</warning>
    print(Int.hashCode())
    print(a)
}