import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test() {
    val foo = remember { Foo() }
    val bar = remember { Foo() }
    A()
    val bam = remember { Foo() }
}
