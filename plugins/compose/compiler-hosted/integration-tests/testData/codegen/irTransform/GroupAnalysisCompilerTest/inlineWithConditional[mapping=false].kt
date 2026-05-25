@Composable
inline fun Test(isEnabled: Boolean, crossinline content: @Composable () -> Unit) {
    if (isEnabled) {
        A()
        return
    }
    content()
}

@Composable
inline fun Test(isEnabled: Boolean, p1: Int, crossinline content: @Composable () -> Unit) {
    Test(isEnabled, content)
}

fun used(x: Any?) {}
