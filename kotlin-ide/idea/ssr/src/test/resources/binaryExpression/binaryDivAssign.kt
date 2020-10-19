class Foo {
    operator fun divAssign(other: Foo) { print(other) }
}

fun foo() {
    var z = 1
    <warning descr="SSR">z /= 2</warning>
    <warning descr="SSR">z = z / 2</warning>
    print(z)

    var x = Foo()
    val y = Foo()
    <warning descr="SSR">x.divAssign(y)</warning>
}