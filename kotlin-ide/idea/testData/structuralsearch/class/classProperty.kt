import java.util.*

class Foo {
    var bar1 = 1
    lateinit var <warning descr="SSR">foo2</warning>: Random
}

class Bar {
    var bar1 = 1
    var bar2 = Random(1)
}