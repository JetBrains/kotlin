import java.util.*

val <caret>property: Int
    get() = Random().nextInt()

fun foo() {
    println(property)
}