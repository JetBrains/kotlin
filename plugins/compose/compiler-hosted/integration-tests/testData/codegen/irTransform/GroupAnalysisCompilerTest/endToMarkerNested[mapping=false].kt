@Composable
fun Test(text: String) {
    Wrapper {
        if (text.isEmpty()) return@Test
        Text(text)

        SecondWrapper {
            if (text.isEmpty()) return@Wrapper
            Text(text)
        }
    }
}

fun used(x: Any?) {}
