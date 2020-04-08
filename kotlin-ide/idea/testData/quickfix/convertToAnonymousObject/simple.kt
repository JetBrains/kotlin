// "Convert to anonymous object" "true"
interface I {
    fun foo(): String
}

fun test() {
    val i = <caret>I { "" }
}