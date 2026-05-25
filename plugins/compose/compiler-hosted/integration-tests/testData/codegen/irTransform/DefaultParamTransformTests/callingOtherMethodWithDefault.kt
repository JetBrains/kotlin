import androidx.compose.runtime.*


@Composable fun Main() {
    Impl.B()
}

interface A {
    @Composable
    fun A(
        default: () -> Float = { 0f },
    ) { }
}

interface B {
    @Composable
    fun B(param: String = "") = Impl.A()
}

interface Combined : A, B
object Impl : Combined
