@Composable
fun Test(param: Value): String {
    Str {
        when(param) {
            Value.A -> "A"
            Value.B -> "B"
        }
    } 

    return Test(param)
}

fun used(x: Any?) {}
