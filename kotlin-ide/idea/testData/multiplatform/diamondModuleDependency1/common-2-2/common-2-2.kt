@file:Suppress("UNUSED_PARAMETER")

package sample

actual interface <!LINE_MARKER("descr='Is implemented by B'"), LINE_MARKER("descr='Has declaration in common module'")!>A<!> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>foo<!>()
    fun baz()
}

fun take_A_common_2_2(x: A) {
    x.foo()
    x.baz()
}