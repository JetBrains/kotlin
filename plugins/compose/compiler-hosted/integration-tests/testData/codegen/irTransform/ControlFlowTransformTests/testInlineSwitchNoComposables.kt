import androidx.compose.runtime.*

@Composable fun Test(clicked: Boolean) {
    thenIf(
        condition = clicked,
        ifTrue = {
            "true"
        },
        ifFalse = {
            "false"
        },
    )
}
