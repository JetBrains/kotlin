@Composable
fun Content(index: Int) {}

@Composable
fun Test() {
    for (i in 0..10) {
        Content(i)
    }

    Content(1)
}

fun used(x: Any?) {}
