import androidx.compose.runtime.Composable

@Composable
fun AttemptedToRealizeGroupTwice() {
    Wrapper {
        repeat(1) {
            repeat(1) {
                Leaf(0)
            }
            Leaf(0)
        }
    }
}
