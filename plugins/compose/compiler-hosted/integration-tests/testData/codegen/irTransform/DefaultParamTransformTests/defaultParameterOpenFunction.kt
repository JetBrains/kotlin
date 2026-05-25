import androidx.compose.runtime.*

            import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

open class Test {
    @Composable open fun doSomething(value: Int = remember { 0 }) {}
}

class TestImpl : Test() {
    @Composable override fun doSomething(value: Int) {
        super.doSomething(value)
    }
}
