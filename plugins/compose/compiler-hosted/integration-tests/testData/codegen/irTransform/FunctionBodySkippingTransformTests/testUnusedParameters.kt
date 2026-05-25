import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


class Unstable(var count: Int)
class Stable(val count: Int)
interface MaybeStable

@Composable
fun Unskippable(a: Unstable, b: Stable, c: MaybeStable) {
    used(a)
}
@Composable
fun Skippable1(a: Unstable, b: Stable, c: MaybeStable) {
    used(b)
}
@Composable
fun Skippable2(a: Unstable, b: Stable, c: MaybeStable) {
    used(c)
}
@Composable
fun Skippable3(a: Unstable, b: Stable, c: MaybeStable) { }
