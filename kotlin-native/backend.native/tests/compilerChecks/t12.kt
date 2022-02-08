import kotlinx.cinterop.*

fun foo(x: CValue<*>?) = x

fun bar() {
    staticCFunction(::foo)
}
