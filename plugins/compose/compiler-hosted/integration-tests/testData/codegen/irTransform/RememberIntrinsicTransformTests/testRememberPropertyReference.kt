import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(a: A) {
    used(remember(a, a::value))
}
