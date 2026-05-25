import androidx.compose.runtime.*

@Composable
fun Fn(int: Int): Int = 0

fun interface IntToInt {
    @Composable
    fun invoke(int: Int): Int
}
