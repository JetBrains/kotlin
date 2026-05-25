import androidx.compose.runtime.Composable

@Composable
fun A() { }

@Composable
fun Text(text: String) { }

@Composable
inline fun Wrapper(content: @Composable () -> Unit) = content()

@Composable
inline fun M1(content: @Composable () -> Unit) = Wrapper {
    content()
}

@Composable
inline fun M2(content: @Composable () -> Unit) = Wrapper {
    Wrapper {
        content()
    }
}

@Composable
inline fun M3(content: @Composable () -> Unit) = Wrapper {
    Wrapper {
        Wrapper {
            content()
        }
    }
}

inline fun <T> Identity(block: () -> T): T = block()

@Composable
fun Stack(content: @Composable () -> Unit) = content()
