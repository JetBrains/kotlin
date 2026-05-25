@Composable
fun Test(text: String) {
    Wrapper {
        if (text.isEmpty()) return@Test
        Text(text)
    }
}

fun used(x: Any?) {}
