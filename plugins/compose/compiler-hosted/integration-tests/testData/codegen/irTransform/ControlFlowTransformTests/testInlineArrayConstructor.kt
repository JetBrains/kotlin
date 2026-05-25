import androidx.compose.runtime.*

@Composable
fun ArrayConstructorTest(n: Int) {
    Array(n) { remember { it } }
    ByteArray(n) { remember { it.toByte() } }
    CharArray(n) { remember { it.toChar() } }
    ShortArray(n) { remember { it.toShort() } }
    IntArray(n) { remember { it } }
    LongArray(n) { remember { it.toLong() } }
    FloatArray(n) { remember { it.toFloat() } }
    DoubleArray(n) { remember { it.toDouble() } }
    BooleanArray(n) { remember { false } }
}
