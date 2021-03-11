val String.foo: Int get() = 1

<warning descr="SSR">val Int.foo: Int get() = 1</warning>
<warning descr="SSR">val kotlin.Int.bar: Int get() = 1</warning>

class A {
    class Int

    val Int.foo: kotlin.Int get() = 1
}
