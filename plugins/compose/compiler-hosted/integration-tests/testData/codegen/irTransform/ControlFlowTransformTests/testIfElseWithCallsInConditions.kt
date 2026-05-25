import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // Since the condition in the else-if is conditionally executed, it means we have
    // dynamic execution and we can't statically guarantee the number of groups. As a
    // result, we generate a group around the if statement in addition to a group around
    // each of the conditions with composable calls in them. Note that no group is
    // needed around the else condition
    if (B(a)) {
        NA()
    } else if (B(b)) {
        NA()
    } else {
        NA()
    }
}
