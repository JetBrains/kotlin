import kotlin.reflect.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
inline fun <reified T : Comparable<T>> foo() {
    typeOf<List<T>>()
}

