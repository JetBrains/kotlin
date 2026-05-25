import androidx.compose.runtime.*

@Composable fun Test(clicked: Boolean) {
    Modifier.thenIf(
        condition = clicked,
        ifTrue = {
           clickable { println(clicked) }
        },
        ifFalse = {
            remember { mutableStateOf(value = "Mock") }
            if (clicked) {
                return
            }
            clickable { println(clicked) }
        },
    )
}
