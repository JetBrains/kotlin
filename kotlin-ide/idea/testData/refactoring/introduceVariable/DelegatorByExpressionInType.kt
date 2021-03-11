interface T

object O : T

fun foo() {
    val x = object : <selection>T</selection> by O {}
}