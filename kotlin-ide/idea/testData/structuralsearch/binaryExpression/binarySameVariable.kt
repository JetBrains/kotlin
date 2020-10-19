fun main() {
    val foo = 1
    val bar = 1
    print(foo + 1)
    print(<warning descr="SSR">foo + foo</warning>)
    print(foo + bar)
}