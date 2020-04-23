fun main() {
    val foo = 1
    <warning descr="SSR">var bar = 1</warning>
    print(foo + bar)
}
