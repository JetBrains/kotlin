import androidx.compose.runtime.Composable

@Composable
fun Test() {
    Wrap {
        repeat(number) {
            effects[it] = effect { 0 }
        }
        outside = effect { "0" }
    }
}
