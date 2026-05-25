import androidx.compose.runtime.*

@Composable
fun Foo() {
    val a: @Composable () -> Unit = { }
    val b: @Composable (x: Int) -> Unit = {}
    val c: @Composable (x: String) -> Unit = {}
    val d: @Composable Int.() -> Unit = {}
}
