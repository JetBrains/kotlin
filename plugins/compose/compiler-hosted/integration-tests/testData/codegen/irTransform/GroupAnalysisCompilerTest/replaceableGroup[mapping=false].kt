@Composable fun <T> Test(text: T): T {
    Text(text.toString())
    return text
}

fun used(x: Any?) {}
