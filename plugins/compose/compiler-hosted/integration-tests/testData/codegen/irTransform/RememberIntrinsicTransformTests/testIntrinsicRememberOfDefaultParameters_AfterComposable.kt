import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(a: Int = remember { 0 }, b: Int = SomeComposable(), c: Int = remember { 0 }) {
    used(a)
    used(b)
    used(c)
}
