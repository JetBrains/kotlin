// A composable SAM conversion in an inline function referencing its reified type parameter.
// The Wasm backend crashed on it ("Invalid type") when it was left to the backend (CMP-9447)

import androidx.compose.runtime.Composable

fun interface Renderer {
    @Composable
    fun render(text: String)
}

inline fun <reified T : Any> typedRenderer() = Renderer { T::class }

fun box(): String {
    typedRenderer<String>()
    return "OK"
}
