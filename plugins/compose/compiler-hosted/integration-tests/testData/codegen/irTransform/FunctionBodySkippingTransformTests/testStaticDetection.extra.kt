import androidx.compose.runtime.Composable


import androidx.compose.runtime.Stable

enum class Foo {
    Bar,
    Bam
}
const val constInt: Int = 123
@Composable fun C(x: Any?) {}
@Stable
interface Modifier {
  companion object : Modifier { }
}
inline class Dp(val value: Int)
@Stable
fun stableFun(x: Int): Int = x * x
@Stable
operator fun Dp.plus(other: Dp): Dp = Dp(this.value + other.value)
@Stable
val Int.dp: Dp get() = Dp(this)
@Composable fun D(content: @Composable() () -> Unit) {}

fun used(x: Any?) {}
