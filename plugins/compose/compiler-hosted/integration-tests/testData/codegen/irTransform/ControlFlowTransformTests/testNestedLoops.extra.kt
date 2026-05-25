import androidx.compose.runtime.Composable

inline class InlineClass(val value: Int)
fun used(x: Any?) {}

@Composable fun A() {}
@Composable fun A(x: Int) { }
@Composable fun B(): Boolean { return true }
@Composable fun B(x: Int): Boolean { return true }
@Composable fun R(): Int { return 10 }
@Composable fun R(x: Int): Int { return 10 }
@Composable fun P(x: Int) { }
@Composable fun Int.A() { }
@Composable fun L(): List<Int> { return listOf(1, 2, 3) }
@Composable fun W(content: @Composable () -> Unit) { content() }
@Composable inline fun IW(content: @Composable () -> Unit) { content() }
fun NA() { }
fun NB(): Boolean { return true }
fun NR(): Int { return 10 }
var a = 1
var b = 2
var c = 3
