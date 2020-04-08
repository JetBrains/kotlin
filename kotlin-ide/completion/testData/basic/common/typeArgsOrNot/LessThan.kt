val v = 1
fun f() = 2

object HashMap

fun foo() {
    val v = HashMap<<caret>
}

// EXIST: String
// EXIST: kotlin
// EXIST: v
// EXIST: f
