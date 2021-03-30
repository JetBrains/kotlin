import kotlin.reflect.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
inline fun <T : Comparable<T>> foo() {
    typeOf<List<T>>()
}

