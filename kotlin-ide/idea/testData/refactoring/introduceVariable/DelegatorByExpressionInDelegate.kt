interface T

object O : T

fun foo() {
    val x = object : T by <selection>O</selection> {}
}