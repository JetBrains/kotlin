class Foo(val a: Int, val b: Int)

class Bar(val f: Foo)

class IntWrapper(val n: Int) {
    val succ: Int get() {
        return n + 1
    }
}

class IntWrapperContainer(val i: IntWrapper)

fun <!VIPER_TEXT!>testGetter<!>() {
    val wrapper = IntWrapper(42)
    val succ = wrapper.succ
}

fun <!VIPER_TEXT!>testCascadeGetter<!>() {
    val foo = Foo(10, 20)
    val bar = Bar(foo)

    val bfa = bar.f.a
    val bfb = bar.f.b
}

fun <!VIPER_TEXT!>testCascadeCustomGetters<!>() {
    val wrapper = IntWrapperContainer(IntWrapper(42))
    val succ = wrapper.i.succ
}