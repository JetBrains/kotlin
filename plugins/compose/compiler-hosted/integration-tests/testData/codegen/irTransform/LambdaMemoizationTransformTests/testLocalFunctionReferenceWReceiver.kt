import androidx.compose.runtime.Composable

@Composable
fun Something(param: String, rcvr: Int) {
    fun Int.method() {
        println(param)
    }
    val x = rcvr::method
}
