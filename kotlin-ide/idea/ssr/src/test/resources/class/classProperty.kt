import java.util.*

class Foo {
    lateinit var <warning descr="SSR">foo2</warning>: Random
    var bar1 = 1
}

class Bar {
    var bar1 = 1
    var bar2 = Random(1)
}