import kotlinx.cinterop.*

fun bar(x: Int) = x.narrow<Long>()
