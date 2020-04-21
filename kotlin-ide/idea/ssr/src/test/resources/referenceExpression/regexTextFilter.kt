fun main() {
    var foo = 0
    var foo1 = 1
    var foo2 = 1
    print(foo)
    print(<warning descr="SSR">foo1</warning>)
    print(<warning descr="SSR">foo2</warning>)
}
