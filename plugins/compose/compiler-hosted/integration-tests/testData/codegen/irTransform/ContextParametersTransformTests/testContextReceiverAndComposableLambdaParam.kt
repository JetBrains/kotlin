// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


    context(_: Foo)
    @Composable
    fun Test(a: String, b: @Composable (String) -> Unit) {
        b("yay")
    }
