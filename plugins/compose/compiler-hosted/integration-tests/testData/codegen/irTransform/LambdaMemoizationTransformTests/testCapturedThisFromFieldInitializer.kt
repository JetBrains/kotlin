import androidx.compose.runtime.Composable

class A {
    val b = ""
    val c = @Composable {
        print(b)
    }
}
