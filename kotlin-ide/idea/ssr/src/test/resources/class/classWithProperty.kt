import java.util.*

<warning descr="SSR">class Foo {
    var foo1 = 1
    lateinit var foo2: Random
}</warning>

class Bar {
    var bar1 = 1
    var bar2 = Random(1)
}