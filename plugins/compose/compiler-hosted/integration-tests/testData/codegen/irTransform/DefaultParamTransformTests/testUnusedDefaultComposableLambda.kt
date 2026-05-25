import androidx.compose.runtime.*


inline fun Bar(unused: @Composable () -> Unit = { }) {}
fun Foo() { Bar() }
