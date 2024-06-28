fun foo() {
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    nlib.sample(5)
}