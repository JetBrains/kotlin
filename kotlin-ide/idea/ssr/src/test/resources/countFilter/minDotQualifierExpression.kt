class A {
    companion object {
        const val FOO = 3.14
    }
}

fun main() {
    val a = <warning descr="SSR">A.FOO</warning>
    <warning descr="SSR">print(<warning descr="SSR">Int.hashCode()</warning>)</warning>
    <warning descr="SSR">print(<warning descr="SSR">a</warning>)</warning>
}