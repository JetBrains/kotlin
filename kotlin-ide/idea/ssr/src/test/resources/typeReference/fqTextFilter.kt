<warning descr="SSR">fun foo1(x : Int) { print(x) }</warning>
<warning descr="SSR">fun foo2(x : kotlin.Int) { print(x) }</warning>

class X {
    class Int

    fun bar(x : Int) { print(x) }
}