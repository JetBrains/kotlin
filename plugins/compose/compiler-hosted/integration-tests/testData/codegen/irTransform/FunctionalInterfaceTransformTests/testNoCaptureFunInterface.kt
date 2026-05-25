import androidx.compose.runtime.*

fun interface Consumer {
    fun consume(t: Int)
}

@Composable fun Test(int: Int) {
    Example {
        println(it)
    }
}

@Composable inline fun Example(consumer: Consumer) {
}
