import androidx.compose.runtime.*
import kotlin.reflect.KProperty

fun interface ThemeToken<T> {

    @Composable
    @ReadOnlyComposable
    fun MaterialTheme.resolve(): T

    @Composable
    @ReadOnlyComposable
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = MaterialTheme.resolve()
}

@get:Composable
val background by ThemeToken { background }
