@Composable fun Test(text: String) {
    if (Local.current) return
    if (text.isEmpty()) return

    Text(text)
}

fun used(x: Any?) {}
