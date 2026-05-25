import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


import androidx.compose.runtime.NonRestartableComposable

class KnownStable
class KnownUnstable(var x: Int)
interface Uncertain

@Composable
@NonRestartableComposable
fun test1(x: KnownStable) {
    remember(x) { 1 }
}
@Composable
@NonRestartableComposable
fun test2(x: KnownUnstable) {
    remember(x) { 1 }
}
@Composable
@NonRestartableComposable
fun test3(x: Uncertain) {
    remember(x) { 1 }
}
