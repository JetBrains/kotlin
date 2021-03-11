// "Surround with null check" "true"

operator fun Int.invoke() = this

fun foo(arg: Int?) {
    <caret>arg()
}