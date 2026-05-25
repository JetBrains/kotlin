import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


fun LazyListScope.Example(items: LazyPagingItems<User>) {
    itemsIndexed(items) { index, user ->
        print("Hello World")
    }
}
