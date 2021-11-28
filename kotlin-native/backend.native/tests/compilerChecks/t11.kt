import kotlinx.cinterop.*

fun foo(f: Function1<in Int, Int>) = f

fun bar() {
    staticCFunction(::foo)
}
