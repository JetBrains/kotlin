@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun failingAssertion() {
    assert(false) { "This assertion must not be removed from the cached library" }
}
