class Foo {
    fun bar0(cb: (result: Int) -> Unit) {}
    fun bar1(cb: (result: @ParameterName("a") Int) -> Unit) {}
    fun bar2(cb: (a: Int, b: Int, c: String) -> Unit) {}
    fun bar3(cb: (int: Int, int_: Int, double: Double) -> Unit) {}
}

fun Foo.bar4(cb: (int: Int, message: String) -> Unit)