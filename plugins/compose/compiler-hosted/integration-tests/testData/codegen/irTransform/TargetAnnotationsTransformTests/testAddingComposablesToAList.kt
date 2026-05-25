import androidx.compose.runtime.*

class Scope {
    private val list = IntervalList<Scope.(Int) -> (@Composable () -> Unit)>()
    fun item(content: @Composable Scope.() -> Unit) {
        list.add(1) { @Composable { content() } }
    }
}
