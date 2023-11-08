// EXIT_CODE: !0
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

// TODO: Test running this test with disabled assertions. This requires removing always-enabled
//       -enable-assertions from `BasicCompilation`.
fun main() {
    assert(false)
}