fun myFun() {}
<warning descr="SSR">fun Int.myFun1() {}</warning>
<warning descr="SSR">fun kotlin.Int.myFun2() {}</warning>

class A {
    class Int
    fun Int.foo() {}
}