import androidx.compose.runtime.*

fun interface Consumer {
    fun consume(t: Int)
}

@Composable fun Test(int: Int) {
    Example {
        println(int)
    }
}

@Composable inline fun Example(consumer: Consumer) {
}
