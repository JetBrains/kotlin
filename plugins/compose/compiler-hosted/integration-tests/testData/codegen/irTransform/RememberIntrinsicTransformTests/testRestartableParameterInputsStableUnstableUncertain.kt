import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


class KnownStable
class KnownUnstable(var x: Int)
interface Uncertain

@Composable
fun test1(x: KnownStable) {
    remember(x) { 1 }
}
@Composable
fun test2(x: KnownUnstable) {
    remember(x) { 1 }
}
@Composable
fun test3(x: Uncertain) {
    remember(x) { 1 }
}
