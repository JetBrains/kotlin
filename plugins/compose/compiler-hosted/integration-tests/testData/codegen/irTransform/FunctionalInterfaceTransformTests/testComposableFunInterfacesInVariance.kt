import androidx.compose.runtime.*

fun interface Consumer<T> {
    @Composable fun consume(t: T)
}

class Repro<T : Any> {
    fun test(consumer: Consumer<in T>) {}
}

fun test() {
    Repro<String>().test { string ->
        println(string)
    }
}
