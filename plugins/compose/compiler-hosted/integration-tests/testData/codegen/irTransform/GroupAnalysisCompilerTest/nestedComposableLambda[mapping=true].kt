@Composable fun Test(param1: String, param2: String) {
    Content {
        Content {
            Text(param1)
            Text(param2)
        }
    }
}

fun used(x: Any?) {}
