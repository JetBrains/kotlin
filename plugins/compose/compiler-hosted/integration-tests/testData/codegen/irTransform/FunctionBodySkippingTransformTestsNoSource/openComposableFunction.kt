import androidx.compose.runtime.*

open class Open {
    @Composable open fun Test() {}
}

class Impl : Open() {
    @Composable override fun Test() {
        super.Test()
    }
}

open class OpenImpl : Open() {
    @Composable override fun Test() {
        super.Test()
    }
}
