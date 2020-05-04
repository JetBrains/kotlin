class A {
    companion object {
        const val FOO = 3.14
    }
}

fun main() {
    val a = <warning descr="SSR">A.FOO</warning>
    print(<warning descr="SSR">Int.hashCode()</warning>)
    print(a)
}