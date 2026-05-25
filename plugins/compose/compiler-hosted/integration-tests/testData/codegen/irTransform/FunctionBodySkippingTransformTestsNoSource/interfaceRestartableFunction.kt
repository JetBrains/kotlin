import androidx.compose.runtime.*

interface Presenter {
    @Composable fun Content()
}

class PresenterImpl(
    private val onCompose: () -> Unit
) : Presenter {
    @Composable
    override fun Content() {
        onCompose()
    }
}
