import kotlinx.cinterop.*

fun foo(f: Function0<*>) = f

fun bar() {
    staticCFunction(::foo)
}
