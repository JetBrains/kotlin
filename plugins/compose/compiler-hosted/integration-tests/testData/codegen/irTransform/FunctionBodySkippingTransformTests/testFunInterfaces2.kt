import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Button(colors: ButtonColors) {
    Text("hello world", color = colors.getColor())
}
@Composable
fun Test() {
    Button {
        if (condition()) Color.Red else Color.Blue
    }
}
