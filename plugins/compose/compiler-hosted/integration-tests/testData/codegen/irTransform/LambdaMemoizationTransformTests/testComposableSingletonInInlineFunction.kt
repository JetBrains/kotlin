import androidx.compose.runtime.Composable

@Composable
inline fun Test1(block: @Composable (@Composable () -> Unit) -> Unit) {
    val x: @Composable () -> Unit = @Composable { }
    val y: @Composable () -> String = @Composable { "hello" }
    val z = @Composable { x }
    block(x)
    block { "Hello" }
}

@Composable
private inline fun Test2(block: @Composable (@Composable () -> Unit) -> Unit) {
    val x: @Composable () -> Unit = @Composable { }
    val y: @Composable () -> String = @Composable { "hello" }
    val z = @Composable { x }
    block(x)
    block { "Hello" }
}

@Composable
internal inline fun Test3(block: @Composable (@Composable () -> Unit) -> Unit) {
    val x: @Composable () -> Unit = @Composable { }
    val y: @Composable () -> String = @Composable { "hello" }
    val z = @Composable { x }
    block(x)
    block { "Hello" }
}

@Composable
@PublishedApi
internal inline fun Test4(block: @Composable (@Composable () -> Unit) -> Unit) {
    val x: @Composable () -> Unit = @Composable { }
    val y: @Composable () -> String = @Composable { "hello" }
    val z = @Composable { x }
    block(x)
    block { "Hello" }
}
