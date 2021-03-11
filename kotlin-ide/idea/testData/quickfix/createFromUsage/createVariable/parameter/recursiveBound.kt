// "Create parameter 'foo'" "true"
class Cyclic<E : Cyclic<E>>

fun test() {
    val c : Cyclic<*> = <caret>foo
}