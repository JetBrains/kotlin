@Composable
fun targetEnterExit(param: String): String =
    key(param) {
        if (param.isEmpty()) {
            "Empty"
        } else {
            val state = remember { mutableStateOf("") }
            state.value
        }
    }

fun used(x: Any?) {}
