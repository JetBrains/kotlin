class X {
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
}

class Y {
    <warning descr="SSR">open class Foo<T> {
        val x = 1
    }</warning>

    <warning descr="SSR">open class Foo2<T> : Foo<T>() {
        val y = 2
    }</warning>

    <warning descr="SSR">class Foo3<Int> : Foo2<Int>() {
        val z = 3
    }</warning>

    class Bar {
        val bar = 0
    }
}