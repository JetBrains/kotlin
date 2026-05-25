import androidx.compose.runtime.*

val Local = staticCompositionLocalOf { null }

@Composable fun Test(clicked: Boolean) {
    thenIf(
        condition = clicked,
        ifTrue = {
            Local.current
        },
        ifFalse = {
            Local.current
        },
    )
}
