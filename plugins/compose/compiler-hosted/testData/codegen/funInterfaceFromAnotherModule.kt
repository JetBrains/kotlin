// DUMP_IR

// A composable fun interface from another module whose method nothing here calls: the implementation
// overrides it, so the method still needs its composer params (CMP-9456)

// MODULE: lib
import androidx.compose.runtime.Composable

fun interface Renderer {
    @Composable
    fun render(text: String)
}

// MODULE: main(lib)
import androidx.compose.runtime.Composable

@Composable
fun Text(text: String) {}

fun renderer(prefix: String) = Renderer { text -> Text(prefix + text) }
