fun main() {
    <warning descr="SSR">val foo = 1</warning>
    val foo2: Int
    var bar: Int = 1
    foo2 = 1
    <warning descr="SSR">val bar2: Int = 1</warning>
    <warning descr="SSR">val bar3: Int = (1)</warning>
    <warning descr="SSR">val bar4: Int = (((1)))</warning>
    print(foo + foo2 + bar + bar2 + bar3 + bar4)
}
