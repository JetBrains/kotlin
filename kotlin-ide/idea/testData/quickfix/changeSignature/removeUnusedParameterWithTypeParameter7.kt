// "Remove parameter 'x'" "true"
interface TypeHolder<T, U>

fun <T, U> baz(<caret>x: TypeHolder<T, U>) {}

fun test(holder: TypeHolder<String, Int>) {
    baz(holder)
}