import androidx.compose.runtime.*

fun interface Consumer {
    @Composable operator fun invoke(t: Int)
}

@Composable fun Test(int: Int) {
    Example { _ ->
    }
}

@Composable fun Example(consumer: Consumer) {
}
