@Composable
fun Test(text: String) {
    if (text.isEmpty()) return
    Text(text)
}

fun used(x: Any?) {}
