<warning descr="SSR">open class Foo {
    val x = 1
}</warning>

<warning descr="SSR">open class Foo2: Foo() {
    val y = 2
}</warning>

<warning descr="SSR">class Foo3 : Foo2() {
    val z = 3
}</warning>

class Bar {
    val bar = 0
}