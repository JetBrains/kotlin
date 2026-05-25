import androidx.compose.runtime.Composable

class ScrollState {
    fun test(index: Int, default: Int = 0): Int = 0
    fun testExact(index: Int): Int = 0
}
fun scrollState(): ScrollState = TODO()

@Composable fun rememberFooInline() = fooInline(scrollState()::test)
@Composable fun rememberFoo() = foo(scrollState()::test)
@Composable fun rememberFooExactInline() = fooInline(scrollState()::testExact)
@Composable fun rememberFooExact() = foo(scrollState()::testExact)

@Composable
inline fun fooInline(block: (Int) -> Int) = block(0)

@Composable
fun foo(block: (Int) -> Int) = block(0)
