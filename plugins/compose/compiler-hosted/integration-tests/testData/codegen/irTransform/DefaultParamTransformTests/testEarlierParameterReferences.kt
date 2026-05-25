import androidx.compose.runtime.*


@Composable
fun A(a: Int = 0, b: Int = a + 1) {
    print(a)
    print(b)
}
