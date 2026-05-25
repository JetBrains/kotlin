import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


import androidx.compose.runtime.currentComposer

open class Foo {
    inline val current: Int
        @Composable
        @ReadOnlyComposable get() = currentComposer.hashCode()

    @ReadOnlyComposable
    @Composable
    fun getHashCode(): Int = currentComposer.hashCode()
}

@ReadOnlyComposable
@Composable
fun getHashCode(): Int = currentComposer.hashCode()
