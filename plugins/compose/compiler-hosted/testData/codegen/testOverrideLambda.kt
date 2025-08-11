// DUMP_IR

// MODULE: lib
import androidx.compose.runtime.Composable

interface ITest {
    @Composable
    fun test(lambda: () -> Unit)
}

class Test : ITest {
    @Composable
    override fun test(lambda: () -> Unit) {
        println("in test")
    }
}

// MODULE: main(lib)
import androidx.compose.runtime.Composable

@Composable
fun Content() {
    Test().test {}
}