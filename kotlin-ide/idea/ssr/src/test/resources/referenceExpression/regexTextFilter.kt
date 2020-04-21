fun main() {
    var foo = 0
    <warning descr="SSR">var foo1 = 1</warning>
    <warning descr="SSR">var foo2 = 1</warning>
    print(foo)
    print(<warning descr="SSR">foo1</warning>)
    print(<warning descr="SSR">foo2</warning>)
}
