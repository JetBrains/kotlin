// IS_APPLICABLE: false
class X<T>(val t: T)

fun <T> X<T>.getT(): T = t

fun <T> useX(x: X<out T>) {
    x.getT<caret>()
}