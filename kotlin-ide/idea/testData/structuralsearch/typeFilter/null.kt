fun foo(x: Int?): Int? { return x }

fun main() {
    foo(1)
    <warning descr="SSR">foo(null)</warning>
}