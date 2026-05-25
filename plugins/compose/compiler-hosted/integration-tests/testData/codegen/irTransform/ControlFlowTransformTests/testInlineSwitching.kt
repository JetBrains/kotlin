import androidx.compose.runtime.*

@Composable fun Test(clicked: Boolean) {
    thenIf(
        condition = clicked,
        ifTrue = {
            val lambdaTrue = { }
        },
        ifFalse = {
            remember { mutableStateOf(value = "Mock") }
            val lambdaFalse = { }
        },
    )
}
