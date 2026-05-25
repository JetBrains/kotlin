import androidx.compose.runtime.Composable

@Composable inline fun Test(crossinline content: () -> Unit) {
    Box {
        Lazy {
            val items = @Composable { content() }
        }
    }
}

@Composable inline fun TestComposable(crossinline content: @Composable () -> Unit) {
    Box {
        Lazy {
            val items = @Composable { content() }
        }
    }
}

@Composable inline fun TestSuspend(crossinline content: suspend () -> Unit) {
    Box {
        Lazy {
            val items = suspend { content() }
        }
    }
}
