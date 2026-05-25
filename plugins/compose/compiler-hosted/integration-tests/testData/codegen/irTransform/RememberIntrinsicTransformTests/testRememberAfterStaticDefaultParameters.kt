import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(a: Int = 1, b: Foo = Foo.B, c: Int = swizzle(1, 2) ) {
    val s = remember(a, b, c) { Any() }
    used(s)
}
