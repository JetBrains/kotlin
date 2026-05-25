import androidx.compose.runtime.Composable

    
import androidx.compose.runtime.*

data class User(
    val id: Int,
    val name: String
)

interface LazyPagingItems<T> {
    val itemCount: Int
    operator fun get(index: Int): State<T?>
}

@Stable interface LazyListScope {
    fun items(itemCount: Int, itemContent: @Composable LazyItemScope.(Int) -> Unit)
}

@Stable interface LazyItemScope

public fun <T : Any> LazyListScope.itemsIndexed(
    lazyPagingItems: LazyPagingItems<T>,
    itemContent: @Composable LazyItemScope.(Int, T?) -> Unit
) {
    items(lazyPagingItems.itemCount) { index ->
        val item = lazyPagingItems[index].value
        itemContent(index, item)
    }
}

    fun used(x: Any?) {}
