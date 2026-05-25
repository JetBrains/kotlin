import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun <T> loadResourceInternal(
    key: String,
    pendingResource: T? = null,
    failedResource: T? = null
): Boolean {
    val deferred = remember(key, pendingResource, failedResource) {
        123
    }
    return deferred > 10
}
