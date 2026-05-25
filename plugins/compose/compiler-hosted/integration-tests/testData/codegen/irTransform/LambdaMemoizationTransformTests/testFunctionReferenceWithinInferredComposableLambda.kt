import androidx.compose.runtime.Composable

fun Problem() {
    fun foo() { }
    val lambda: @Composable ()->Unit = {
        ::foo
    }
}
