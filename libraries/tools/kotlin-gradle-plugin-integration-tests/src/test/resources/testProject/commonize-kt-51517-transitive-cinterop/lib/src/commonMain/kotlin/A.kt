import dummy.foo

fun dummyMain() {
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    foo()
}
