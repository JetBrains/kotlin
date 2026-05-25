import androidx.compose.runtime.Composable

@Composable
fun Something(param: String) {
    fun method() {
        println(param)
    }
    val x = ::method
}
