interface TypeHolder<T, U>

fun <T, U> <caret>TypeHolder<T, U>.foo() {}

fun test(holder: TypeHolder<String, Int>) {
    holder.foo()
}