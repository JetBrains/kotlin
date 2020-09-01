class A {
    companion object {
        const val FOO = 3.14
    }
}

fun main() {
    val a = A.FOO
    <warning descr="SSR">print(Int.hashCode())</warning>
    <warning descr="SSR">print(<warning descr="SSR">a</warning>)</warning>
}