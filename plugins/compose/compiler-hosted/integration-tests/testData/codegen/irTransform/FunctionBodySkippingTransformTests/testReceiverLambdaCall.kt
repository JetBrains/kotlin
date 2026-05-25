import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


val unstableUnused: @Composable Foo.() -> Unit = {
}
val unstableUsed: @Composable Foo.() -> Unit = {
    used(x)
}
val stableUnused: @Composable StableFoo.() -> Unit = {
}
val stableUsed: @Composable StableFoo.() -> Unit = {
    used(x)
}
