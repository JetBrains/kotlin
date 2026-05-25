import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun StrongSkippingIssue(
    data: ClassWithData
) {
    val action by data::action
    val action1 by getData()::action
    {
        action
    }
    {
        action1
    }
}
