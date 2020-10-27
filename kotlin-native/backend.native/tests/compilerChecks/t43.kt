import kotlinx.cinterop.*

fun bar(x: Float) = x.signExtend<Int>()
