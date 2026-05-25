import androidx.compose.runtime.Composable

class SomeClass {
    var a = "Test"
    fun onCreate() {
        setContent {
            B(a)
            B(a)
        }
    }
}

fun Test() {
    var a = "Test"
    setContent {
        B(a)
        B(a)
    }
}
