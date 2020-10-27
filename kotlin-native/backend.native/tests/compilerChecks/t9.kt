import kotlinx.cinterop.*

fun foo(f: Function0<out Int>) = f

fun bar() {
    staticCFunction(::foo)
}
