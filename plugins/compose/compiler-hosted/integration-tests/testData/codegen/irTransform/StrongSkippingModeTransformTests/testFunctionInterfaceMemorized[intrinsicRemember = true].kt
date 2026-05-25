import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable fun TestMemoizedFun(compute: TestFunInterface) {}
@Composable fun Test() {
    val capture = 0
    TestMemoizedFun {
        // no captures
        use(it)
    }
    TestMemoizedFun {
        // stable captures
        use(capture)
    }
}
