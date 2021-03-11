// "Change type of 't' to 'T'" "true"
interface T

fun foo() {
    val t: Int = <caret>object: T{}
}