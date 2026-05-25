@Composable fun <T> identity(value: T): T = value
@Composable fun Foo(x: Int = identity(20)) {

}
@Composable fun test() {
    Foo()
    Foo(10)
}

fun used(x: Any?) {}
