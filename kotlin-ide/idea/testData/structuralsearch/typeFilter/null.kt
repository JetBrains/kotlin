fun foo(x: Int?) { print(x) }

fun main() {
    foo(1)
    <warning descr="SSR">foo(null)</warning>
}