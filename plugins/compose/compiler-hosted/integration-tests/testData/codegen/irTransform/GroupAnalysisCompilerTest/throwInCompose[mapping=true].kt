@Composable
fun Test(param: String): String =
    TODO("test")

@Composable
fun TestConditional(param: String): String =
    if (param.isEmpty()) {
        Test("")
        TODO("test")
    } else {
        Test(param)
    }

@Composable
fun TestFunctionThrow(param: String): String =
    throwError() // this will generate `throw NothingValueException()`

private fun throwError(): Nothing = TODO()

fun used(x: Any?) {}
